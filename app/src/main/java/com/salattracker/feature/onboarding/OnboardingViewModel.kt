package com.salattracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.core.locale.LocaleManager
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.GeocodingRepository
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.usecase.isPrayerMarkable
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import com.salattracker.core.util.DebouncedCitySearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** The first-run onboarding steps, in order a user moving straight forward sees them. [CITY_SEARCH]
 * and [CATCH_UP_ADD] are branches off [LOCATION_INTRO]/[LOCATION_CONFIRM] and [CATCH_UP] rather
 * than always-visited steps — see [OnboardingViewModel]'s back-stack for how "back" still returns
 * to wherever a screen was actually reached from. */
enum class OnboardingStep {
    WELCOME, ABOUT_YOU, SCHOOL, LOCATION_INTRO, CITY_SEARCH, LOCATION_CONFIRM, REMINDERS, CATCH_UP, CATCH_UP_ADD, DONE
}

/** How far back the Catch Up step looks for missed prayers. Kept separate from
 * feature/bulkupdate's own DurationOption — that one drives a full date-stepping/custom-calendar
 * UI this single onboarding screen doesn't need. */
enum class CatchUpDuration(val days: Int) {
    WEEK(7), MONTH(30), HALF_YEAR(183), CUSTOM(-1),
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,

    // Welcome
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // About you — null until chosen; the step's Continue button stays disabled until then.
    val gender: Gender? = null,

    // School of thought
    val madhab: Madhab = Madhab.STANDARD,

    // Location
    val confirmedCity: CityResult? = null,
    val isFromDeviceLocation: Boolean = true,
    val calculationMethod: CalculationMethod = CalculationMethod.DEFAULT,
    val showCalculationMethodSheet: Boolean = false,
    val isLocating: Boolean = false,
    val locationError: Boolean = false,

    // City search
    val cityQuery: String = "",
    val cityResults: List<CityResult> = emptyList(),
    val isSearchingCity: Boolean = false,

    // Reminders
    val prayerRemindersEnabled: Boolean = true,
    val fullScreenAlertEnabled: Boolean = true,

    // Catch up
    val catchUpDuration: CatchUpDuration = CatchUpDuration.MONTH,
    val catchUpCustomFrom: LocalDate = LocalDate.now().minusDays(29),
    val catchUpCustomTo: LocalDate = LocalDate.now(),
    val isApplyingCatchUp: Boolean = false,
    val catchUpAppliedCount: Int? = null,
    val catchUpUndoAvailable: Boolean = false,

    val isFinishing: Boolean = false,
    /** True while the "Restore from Google Drive" flow (feature/backup's BackupEntry) is shown
     * in place of the normal onboarding steps. A successful restore sets onboarding_completed
     * itself (see BackupLocalSource.applySnapshot), so MainActivity's reactive gate swaps to the
     * main app on its own — no extra signal needed here. */
    val showRestore: Boolean = false,
) {
    /** The actual date range the Catch Up write covers — [CatchUpDuration.CUSTOM] uses the picked
     * from/to dates (inclusive); every other duration is that many days ending today. */
    val catchUpRange: Pair<LocalDate, LocalDate>
        get() = if (catchUpDuration == CatchUpDuration.CUSTOM) {
            catchUpCustomFrom to catchUpCustomTo
        } else {
            val today = LocalDate.now()
            today.minusDays((catchUpDuration.days - 1).toLong()) to today
        }

    val catchUpDays: Int get() = ChronoUnit.DAYS.between(catchUpRange.first, catchUpRange.second).toInt() + 1

    /** 5 prayers/day, matching the mockup's own "N days -> 5N prayers" estimate. */
    val catchUpPrayerCount: Int get() = catchUpDays * PrayerType.values().size
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val geocodingRepository: GeocodingRepository,
    private val prayerRepository: PrayerRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private var catchUpUndoEntries: List<Pair<LocalDate, PrayerType>>? = null

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Steps visited, in order, so [onBack] always returns to wherever the current step was
     * actually reached from — e.g. Location Confirm back-targets Location or City Search
     * depending on which one led here, rather than a fixed previous-enum-value. */
    private val backStack = mutableListOf<OnboardingStep>()

    private val citySearch = DebouncedCitySearch(
        scope = viewModelScope,
        repository = geocodingRepository,
        onSearching = { searching -> _uiState.update { it.copy(isSearchingCity = searching) } },
        onResults = { results -> _uiState.update { it.copy(cityResults = results) } },
    )

    init { observeDeviceLocation() }

    /** The device-location permission flow (launched by the Activity via [onAllowLocationClick]'s
     * caller) writes straight to [SettingsRepository] once a fix lands — see MainActivity's
     * locationPermissionLauncher. Watching for that write here is how Location Intro learns a fix
     * arrived, without this ViewModel owning any permission/location-client code itself. */
    private fun observeDeviceLocation() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { prefs ->
                if (_uiState.value.step != OnboardingStep.LOCATION_INTRO) return@collect
                if (prefs.latitude == 0.0 && prefs.longitude == 0.0) return@collect
                val city = runCatching { geocodingRepository.reverseGeocode(prefs.latitude, prefs.longitude) }
                    .getOrNull()
                    ?: CityResult(name = "", region = null, country = null, latitude = prefs.latitude, longitude = prefs.longitude)
                _uiState.update { it.copy(confirmedCity = city, isFromDeviceLocation = true, isLocating = false, locationError = false) }
                goTo(OnboardingStep.LOCATION_CONFIRM)
            }
        }
    }

    private fun goTo(step: OnboardingStep) {
        backStack.add(_uiState.value.step)
        _uiState.update { it.copy(step = step) }
    }

    fun onBack() {
        val previous = backStack.removeLastOrNull() ?: return
        _uiState.update { it.copy(step = previous) }
    }

    fun onRestoreClick() = _uiState.update { it.copy(showRestore = true) }
    fun onRestoreDismiss() = _uiState.update { it.copy(showRestore = false) }

    // ── Welcome ───────────────────────────────────────────────────────────────

    fun onLanguageSelected(language: AppLanguage) = _uiState.update { it.copy(language = language) }
    fun onThemeSelected(themeMode: ThemeMode) = _uiState.update { it.copy(themeMode = themeMode) }
    fun onWelcomeNext() = goTo(OnboardingStep.ABOUT_YOU)

    // ── About you ─────────────────────────────────────────────────────────────

    fun onGenderSelected(gender: Gender) = _uiState.update { it.copy(gender = gender) }
    fun onAboutYouNext() = goTo(OnboardingStep.SCHOOL)

    // ── School of thought ─────────────────────────────────────────────────────

    fun onMadhabSelected(madhab: Madhab) = _uiState.update { it.copy(madhab = madhab) }
    fun onSchoolNext() = goTo(OnboardingStep.LOCATION_INTRO)

    // ── Location ──────────────────────────────────────────────────────────────

    fun onLocationRequestStarted() = _uiState.update { it.copy(isLocating = true, locationError = false) }
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) _uiState.update { it.copy(isLocating = false, locationError = true) }
    }
    fun onEnterCityInstead() = goTo(OnboardingStep.CITY_SEARCH)

    fun onConfirmLocation() {
        val state = _uiState.value
        val city = state.confirmedCity ?: return
        viewModelScope.launch {
            settingsRepository.setUseDeviceLocation(state.isFromDeviceLocation)
            if (!state.isFromDeviceLocation) {
                settingsRepository.setLocation(city.latitude, city.longitude)
            }
            if (city.name.isNotBlank()) settingsRepository.setLocationLabel(city.fullLabel)
            settingsRepository.setCalculationMethodId(state.calculationMethod.id)
        }
        goTo(OnboardingStep.REMINDERS)
    }

    fun onChangeLocationClick() = goTo(OnboardingStep.CITY_SEARCH)

    fun onCalculationMethodSheetOpen() = _uiState.update { it.copy(showCalculationMethodSheet = true) }
    fun onCalculationMethodSheetDismiss() = _uiState.update { it.copy(showCalculationMethodSheet = false) }
    fun onCalculationMethodSelected(method: CalculationMethod) =
        _uiState.update { it.copy(calculationMethod = method, showCalculationMethodSheet = false) }

    // ── City search ───────────────────────────────────────────────────────────

    fun onCityQueryChanged(query: String) {
        _uiState.update { it.copy(cityQuery = query) }
        citySearch.search(query)
    }

    fun onCitySelected(city: CityResult) = _uiState.update { it.copy(confirmedCity = city, isFromDeviceLocation = false) }

    /** Used by "Use my location instead" — a method switch, not a back-navigation (see the
     * onboarding nav map), so it goes straight to Location Intro without touching the back stack
     * the way [goTo] would (that stack already has Location Intro in it from getting here). */
    fun onUseMyLocationInstead() = _uiState.update { it.copy(step = OnboardingStep.LOCATION_INTRO) }

    fun onCitySearchContinue() {
        if (_uiState.value.confirmedCity == null) return
        goTo(OnboardingStep.LOCATION_CONFIRM)
    }

    // ── Reminders ─────────────────────────────────────────────────────────────

    fun onPrayerRemindersChanged(enabled: Boolean) = _uiState.update { it.copy(prayerRemindersEnabled = enabled) }
    fun onFullScreenAlertChanged(enabled: Boolean) = _uiState.update { it.copy(fullScreenAlertEnabled = enabled) }
    fun onRemindersNext() = goTo(OnboardingStep.CATCH_UP)
    fun onRemindersSkip() {
        _uiState.update { it.copy(prayerRemindersEnabled = false) }
        goTo(OnboardingStep.CATCH_UP)
    }

    // ── Catch up ──────────────────────────────────────────────────────────────

    fun onCatchUpAddClick() = goTo(OnboardingStep.CATCH_UP_ADD)
    fun onCatchUpSkip() = goTo(OnboardingStep.DONE)
    fun onCatchUpDurationChanged(duration: CatchUpDuration) = _uiState.update { it.copy(catchUpDuration = duration) }

    /** Mirrors feature/bulkupdate's own custom-range handling: swap if reversed, and never let
     * either end land after today (there's nothing to mark Qaza in the future). */
    fun onCatchUpCustomRangeChanged(from: LocalDate, to: LocalDate) {
        val today = LocalDate.now()
        val (lo, hi) = if (from.isAfter(to)) to to from else from to to
        _uiState.update { it.copy(catchUpCustomFrom = minOf(lo, today), catchUpCustomTo = minOf(hi, today)) }
    }

    fun onApplyCatchUp() {
        val state = _uiState.value
        val (from, to) = state.catchUpRange
        if (from.isAfter(to)) {
            goTo(OnboardingStep.DONE)
            return
        }
        _uiState.update { it.copy(isApplyingCatchUp = true) }
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val todayTimes = prayerRepository.getPrayerTimes(today)
                val dates = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
                val entries = dates
                    .flatMap { date -> PrayerType.values().map { date to it } }
                    .filter { (date, type) -> isPrayerMarkable(date, todayTimes?.get(type), today) }
                prayerRepository.bulkUpsertPrayerStatus(entries, PrayerStatus.QAZA_PENDING)
                entries.size to entries
            }.onSuccess { count ->
                catchUpUndoEntries = count.second
                _uiState.update { it.copy(isApplyingCatchUp = false, catchUpAppliedCount = count.first, catchUpUndoAvailable = true) }
                goTo(OnboardingStep.DONE)
            }.onFailure {
                _uiState.update { it.copy(isApplyingCatchUp = false) }
            }
        }
    }

    fun onUndoCatchUp() {
        val entries = catchUpUndoEntries ?: return
        catchUpUndoEntries = null
        _uiState.update { it.copy(catchUpUndoAvailable = false, catchUpAppliedCount = null) }
        viewModelScope.launch { runCatching { prayerRepository.bulkUpsertPrayerStatus(entries, PrayerStatus.NOT_RECORDED) } }
    }

    // ── Done ──────────────────────────────────────────────────────────────────

    /**
     * Persists every onboarding choice and marks onboarding complete. Location (if a device fix
     * landed) and the calculation method were already written when Location Confirm was accepted
     * — everything else is written here in one go, matching the one-shot write style the previous
     * (5-step) onboarding used. Setting onboardingCompleted last flips the reactive app-start gate
     * over to the main app.
     */
    fun onFinish() {
        val state = _uiState.value
        _uiState.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            settingsRepository.setLanguage(state.language)
            settingsRepository.setThemeMode(state.themeMode)
            state.gender?.let { settingsRepository.setGender(it) }
            // Also marks madhabChosen = true, so a fresh install never sees the one-time
            // existing-user prompt (MainActivity's MadhabPromptDialog) afterward.
            settingsRepository.setMadhab(state.madhab)
            settingsRepository.setAllRemindersEnabled(state.prayerRemindersEnabled)
            PrayerType.values().forEach { type ->
                settingsRepository.setReminderFullScreenAlert(type, state.fullScreenAlertEnabled)
            }
            // Reschedule prayer reminders to honour the master toggle chosen here.
            PrayerReminderWorker.enqueueOneTime(workManager)
            settingsRepository.setOnboardingCompleted(true)
            // Apply the chosen language app-wide (recreates the UI into that locale).
            LocaleManager.apply(state.language)
        }
    }

}
