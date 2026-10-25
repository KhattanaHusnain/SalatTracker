package com.salattracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
import com.salattracker.domain.repository.PrayerReminderRescheduler
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
    /** Location may suggest a method until the person explicitly selects a different one. */
    val calculationMethodManuallySelected: Boolean = false,
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
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var catchUpUndoEntries: List<Pair<LocalDate, PrayerType>>? = null

    private val _uiState = MutableStateFlow(restoredState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Steps visited, in order, so [onBack] always returns to wherever the current step was
     * actually reached from — e.g. Location Confirm back-targets Location or City Search
     * depending on which one led here, rather than a fixed previous-enum-value. */
    private val backStack = savedStateHandle.get<ArrayList<String>>(KEY_BACK_STACK)
        .orEmpty()
        .mapNotNull { runCatching { OnboardingStep.valueOf(it) }.getOrNull() }
        .toMutableList()

    private val citySearch = DebouncedCitySearch(
        scope = viewModelScope,
        repository = geocodingRepository,
        onSearching = { searching -> updateState { it.copy(isSearchingCity = searching) } },
        onResults = { results -> updateState { it.copy(cityResults = results) } },
    )

    init { observeDeviceLocation() }

    private fun updateState(transform: (OnboardingUiState) -> OnboardingUiState) {
        _uiState.update { current -> transform(current).also(::saveState) }
    }

    private fun saveState(state: OnboardingUiState) {
        savedStateHandle[KEY_STEP] = state.step.name
        savedStateHandle[KEY_LANGUAGE] = state.language.name
        savedStateHandle[KEY_THEME] = state.themeMode.name
        savedStateHandle[KEY_GENDER] = state.gender?.name
        savedStateHandle[KEY_MADHAB] = state.madhab.name
        savedStateHandle[KEY_FROM_DEVICE] = state.isFromDeviceLocation
        savedStateHandle[KEY_CALCULATION] = state.calculationMethod.id
        savedStateHandle[KEY_CALCULATION_MANUALLY_SELECTED] = state.calculationMethodManuallySelected
        savedStateHandle[KEY_CITY_QUERY] = state.cityQuery
        savedStateHandle[KEY_REMINDERS] = state.prayerRemindersEnabled
        savedStateHandle[KEY_FULLSCREEN] = state.fullScreenAlertEnabled
        savedStateHandle[KEY_CATCH_UP_DURATION] = state.catchUpDuration.name
        savedStateHandle[KEY_CATCH_UP_FROM] = state.catchUpCustomFrom.toString()
        savedStateHandle[KEY_CATCH_UP_TO] = state.catchUpCustomTo.toString()
        state.confirmedCity?.let { city ->
            savedStateHandle[KEY_CITY_NAME] = city.name
            savedStateHandle[KEY_CITY_REGION] = city.region
            savedStateHandle[KEY_CITY_COUNTRY] = city.country
            savedStateHandle[KEY_CITY_COUNTRY_CODE] = city.countryCode
            savedStateHandle[KEY_CITY_LATITUDE] = city.latitude
            savedStateHandle[KEY_CITY_LONGITUDE] = city.longitude
        }
        savedStateHandle[KEY_BACK_STACK] = ArrayList(backStack.map { it.name })
    }

    private fun restoredState(): OnboardingUiState {
        fun <T : Enum<T>> enum(key: String, values: Array<T>, fallback: T): T =
            savedStateHandle.get<String>(key)?.let { name -> values.firstOrNull { it.name == name } } ?: fallback
        val defaults = OnboardingUiState()
        val cityName = savedStateHandle.get<String>(KEY_CITY_NAME)
        val city = cityName?.let {
            CityResult(it, savedStateHandle[KEY_CITY_REGION], savedStateHandle[KEY_CITY_COUNTRY],
                savedStateHandle[KEY_CITY_LATITUDE] ?: 0.0, savedStateHandle[KEY_CITY_LONGITUDE] ?: 0.0,
                savedStateHandle[KEY_CITY_COUNTRY_CODE])
        }
        return defaults.copy(
            step = enum(KEY_STEP, OnboardingStep.values(), defaults.step),
            language = enum(KEY_LANGUAGE, AppLanguage.values(), defaults.language),
            themeMode = enum(KEY_THEME, ThemeMode.values(), defaults.themeMode),
            gender = savedStateHandle.get<String>(KEY_GENDER)?.let { name -> Gender.values().firstOrNull { it.name == name } },
            madhab = enum(KEY_MADHAB, Madhab.values(), defaults.madhab),
            confirmedCity = city,
            isFromDeviceLocation = savedStateHandle[KEY_FROM_DEVICE] ?: defaults.isFromDeviceLocation,
            calculationMethod = CalculationMethod.values().firstOrNull { it.id == savedStateHandle.get<Int>(KEY_CALCULATION) } ?: defaults.calculationMethod,
            calculationMethodManuallySelected = savedStateHandle[KEY_CALCULATION_MANUALLY_SELECTED] ?: defaults.calculationMethodManuallySelected,
            cityQuery = savedStateHandle[KEY_CITY_QUERY] ?: "",
            prayerRemindersEnabled = savedStateHandle[KEY_REMINDERS] ?: defaults.prayerRemindersEnabled,
            fullScreenAlertEnabled = savedStateHandle[KEY_FULLSCREEN] ?: defaults.fullScreenAlertEnabled,
            catchUpDuration = enum(KEY_CATCH_UP_DURATION, CatchUpDuration.values(), defaults.catchUpDuration),
            catchUpCustomFrom = savedStateHandle.get<String>(KEY_CATCH_UP_FROM)?.let(LocalDate::parse) ?: defaults.catchUpCustomFrom,
            catchUpCustomTo = savedStateHandle.get<String>(KEY_CATCH_UP_TO)?.let(LocalDate::parse) ?: defaults.catchUpCustomTo,
        )
    }

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
                updateState { state ->
                    state.withSuggestedCalculationMethod(city)
                        .copy(confirmedCity = city, isFromDeviceLocation = true, isLocating = false, locationError = false)
                }
                goTo(OnboardingStep.LOCATION_CONFIRM)
            }
        }
    }

    private fun goTo(step: OnboardingStep) {
        backStack.add(_uiState.value.step)
        updateState { it.copy(step = step) }
    }

    fun onBack() {
        val previous = backStack.removeLastOrNull() ?: return
        updateState { it.copy(step = previous) }
    }

    fun onRestoreClick() = updateState { it.copy(showRestore = true) }
    fun onRestoreDismiss() = updateState { it.copy(showRestore = false) }

    // ── Welcome ───────────────────────────────────────────────────────────────

    fun onLanguageSelected(language: AppLanguage) = updateState { it.copy(language = language) }
    fun onThemeSelected(themeMode: ThemeMode) = updateState { it.copy(themeMode = themeMode) }
    fun onWelcomeNext() = goTo(OnboardingStep.ABOUT_YOU)

    // ── About you ─────────────────────────────────────────────────────────────

    fun onGenderSelected(gender: Gender) = updateState { it.copy(gender = gender) }
    fun onAboutYouNext() = goTo(OnboardingStep.SCHOOL)

    // ── School of thought ─────────────────────────────────────────────────────

    fun onMadhabSelected(madhab: Madhab) = updateState { it.copy(madhab = madhab) }
    fun onSchoolNext() = goTo(OnboardingStep.LOCATION_INTRO)

    // ── Location ──────────────────────────────────────────────────────────────

    fun onLocationRequestStarted() = updateState { it.copy(isLocating = true, locationError = false) }
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) updateState { it.copy(isLocating = false, locationError = true) }
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

    fun onCalculationMethodSheetOpen() = updateState { it.copy(showCalculationMethodSheet = true) }
    fun onCalculationMethodSheetDismiss() = updateState { it.copy(showCalculationMethodSheet = false) }
    fun onCalculationMethodSelected(method: CalculationMethod) =
        updateState { it.copy(calculationMethod = method, calculationMethodManuallySelected = true, showCalculationMethodSheet = false) }

    // ── City search ───────────────────────────────────────────────────────────

    fun onCityQueryChanged(query: String) {
        updateState { it.copy(cityQuery = query) }
        citySearch.search(query)
    }

    fun onCitySelected(city: CityResult) = updateState {
        it.withSuggestedCalculationMethod(city).copy(confirmedCity = city, isFromDeviceLocation = false)
    }

    /** Used by "Use my location instead" — a method switch, not a back-navigation (see the
     * onboarding nav map), so it goes straight to Location Intro without touching the back stack
     * the way [goTo] would (that stack already has Location Intro in it from getting here). */
    fun onUseMyLocationInstead() = updateState { it.copy(step = OnboardingStep.LOCATION_INTRO) }

    fun onCitySearchContinue() {
        if (_uiState.value.confirmedCity == null) return
        goTo(OnboardingStep.LOCATION_CONFIRM)
    }

    private fun OnboardingUiState.withSuggestedCalculationMethod(city: CityResult): OnboardingUiState =
        if (calculationMethodManuallySelected) this
        else copy(calculationMethod = CalculationMethod.suggestedFor(city.countryCode))

    // ── Reminders ─────────────────────────────────────────────────────────────

    fun onPrayerRemindersChanged(enabled: Boolean) = updateState { it.copy(prayerRemindersEnabled = enabled) }
    fun onFullScreenAlertChanged(enabled: Boolean) = updateState { it.copy(fullScreenAlertEnabled = enabled) }
    fun onRemindersNext() = goTo(OnboardingStep.CATCH_UP)
    fun onRemindersSkip() {
        updateState { it.copy(prayerRemindersEnabled = false) }
        goTo(OnboardingStep.CATCH_UP)
    }

    // ── Catch up ──────────────────────────────────────────────────────────────

    fun onCatchUpAddClick() = goTo(OnboardingStep.CATCH_UP_ADD)
    fun onCatchUpSkip() = goTo(OnboardingStep.DONE)
    fun onCatchUpDurationChanged(duration: CatchUpDuration) = updateState { it.copy(catchUpDuration = duration) }

    /** Mirrors feature/bulkupdate's own custom-range handling: swap if reversed, and never let
     * either end land after today (there's nothing to mark Qaza in the future). */
    fun onCatchUpCustomRangeChanged(from: LocalDate, to: LocalDate) {
        val today = LocalDate.now()
        val (lo, hi) = if (from.isAfter(to)) to to from else from to to
        updateState { it.copy(catchUpCustomFrom = minOf(lo, today), catchUpCustomTo = minOf(hi, today)) }
    }

    fun onApplyCatchUp() {
        val state = _uiState.value
        val (from, to) = state.catchUpRange
        if (from.isAfter(to)) {
            goTo(OnboardingStep.DONE)
            return
        }
        updateState { it.copy(isApplyingCatchUp = true) }
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
                updateState { it.copy(isApplyingCatchUp = false, catchUpAppliedCount = count.first, catchUpUndoAvailable = true) }
                goTo(OnboardingStep.DONE)
            }.onFailure {
                updateState { it.copy(isApplyingCatchUp = false) }
            }
        }
    }

    fun onUndoCatchUp() {
        val entries = catchUpUndoEntries ?: return
        catchUpUndoEntries = null
        updateState { it.copy(catchUpUndoAvailable = false, catchUpAppliedCount = null) }
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
        updateState { it.copy(isFinishing = true) }
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
            prayerReminderRescheduler.rescheduleAll()
            settingsRepository.setOnboardingCompleted(true)
            // Apply the chosen language app-wide (recreates the UI into that locale).
            LocaleManager.apply(state.language)
        }
    }

    private companion object {
        const val KEY_STEP = "onboarding.step"
        const val KEY_BACK_STACK = "onboarding.backStack"
        const val KEY_LANGUAGE = "onboarding.language"
        const val KEY_THEME = "onboarding.theme"
        const val KEY_GENDER = "onboarding.gender"
        const val KEY_MADHAB = "onboarding.madhab"
        const val KEY_FROM_DEVICE = "onboarding.fromDevice"
        const val KEY_CALCULATION = "onboarding.calculation"
        const val KEY_CALCULATION_MANUALLY_SELECTED = "onboarding.calculationManuallySelected"
        const val KEY_CITY_QUERY = "onboarding.cityQuery"
        const val KEY_CITY_NAME = "onboarding.city.name"
        const val KEY_CITY_REGION = "onboarding.city.region"
        const val KEY_CITY_COUNTRY = "onboarding.city.country"
        const val KEY_CITY_COUNTRY_CODE = "onboarding.city.countryCode"
        const val KEY_CITY_LATITUDE = "onboarding.city.latitude"
        const val KEY_CITY_LONGITUDE = "onboarding.city.longitude"
        const val KEY_REMINDERS = "onboarding.reminders"
        const val KEY_FULLSCREEN = "onboarding.fullscreen"
        const val KEY_CATCH_UP_DURATION = "onboarding.catchUpDuration"
        const val KEY_CATCH_UP_FROM = "onboarding.catchUpFrom"
        const val KEY_CATCH_UP_TO = "onboarding.catchUpTo"
    }

}
