package com.salattracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.VerseRepository
import com.salattracker.domain.usecase.CalculateStreakUseCase
import com.salattracker.domain.usecase.isPrayerMarkable
import kotlin.math.roundToInt
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayDate: LocalDate = LocalDate.now(),
    val hijriDate: String = "",
    val prayers: List<PrayerEntryUi> = emptyList(),
    val streaks: Map<PrayerType, StreakInfo> = emptyMap(),
    val verse: VerseInfo? = null,
    val goalsCompletionPercent: Int? = null,   // null = no active goals, hide the summary card
    val sheetArgs: PrayerStatusSheetArgs? = null,   // null = sheet hidden
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class PrayerEntryUi(
    val type: PrayerType,
    val time: LocalTime?,
    val reminderTime: LocalTime?,   // prayer time adjusted by the reminder offset; null if no offset/reminder disabled
    val status: PrayerStatus,
    val isMarkable: Boolean,        // false once it's a prayer later today that hasn't happened yet
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val verseRepository: VerseRepository,
    private val goalRepository: GoalRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayPrayers()
        loadStreak()
        loadVerse()
        loadGoalsSummary()
        PrayerReminderWorker.enqueueOneTime(workManager)
    }

    private fun loadGoalsSummary() {
        viewModelScope.launch {
            goalRepository.observeActiveGoalsWithProgress().collect { goals ->
                val percent = if (goals.isEmpty()) {
                    null
                } else {
                    (goals.sumOf { it.progressFraction.toDouble() } / goals.size * 100).roundToInt()
                }
                _uiState.update { it.copy(goalsCompletionPercent = percent) }
            }
        }
    }

    private fun loadVerse() {
        viewModelScope.launch {
            _uiState.update { it.copy(verse = verseRepository.getRandomVerse()) }
        }
    }

    private fun loadTodayPrayers() {
        viewModelScope.launch {
            val today = LocalDate.now()

            prayerRepository.observeTodayPrayers(today)
                .combine(settingsRepository.observeSettings()) { entries, prefs -> entries to prefs }
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (entries, prefs) ->
                    val times = prayerRepository.getCachedPrayerTimes(today)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            todayDate = today,
                            prayers = PrayerType.values().map { type ->
                                val prayerTime      = times?.get(type)
                                val offsetMinutes    = prefs.reminderOffsetMinutes[type] ?: 0
                                val reminderEnabled  = prefs.reminderEnabled[type] ?: true
                                PrayerEntryUi(
                                    type         = type,
                                    time         = prayerTime,
                                    reminderTime = if (reminderEnabled && offsetMinutes != 0)
                                        prayerTime?.plusMinutes(offsetMinutes.toLong())
                                    else null,
                                    status = entries.find { it.prayerType == type }?.status
                                        ?: PrayerStatus.NOT_RECORDED,
                                    isMarkable = isPrayerMarkable(today, prayerTime),
                                )
                            },
                        )
                    }
                }
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            calculateStreakUseCase().collect { streaks ->
                _uiState.update { it.copy(streaks = streaks) }
            }
        }
    }

    // ── Sheet ─────────────────────────────────────────────────────────────────

    fun onPrayerCardClick(type: PrayerType) {
        val entry = _uiState.value.prayers.find { it.type == type } ?: return
        if (!entry.isMarkable) return
        val current = entry.status
        _uiState.update {
            it.copy(
                sheetArgs = PrayerStatusSheetArgs(
                    prayerType    = type,
                    date          = it.todayDate,
                    currentStatus = current,
                )
            )
        }
    }

    fun onSheetDismiss() = _uiState.update { it.copy(sheetArgs = null) }

    fun onStatusSave(newStatus: PrayerStatus) {
        val args = _uiState.value.sheetArgs ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                prayerRepository.upsertPrayerStatus(args.date, args.prayerType, newStatus)
            }.onSuccess {
                // Streak tiles refresh on their own — loadStreak()'s Flow collector
                // re-emits automatically once upsertPrayerStatus's write commits.
                _uiState.update { it.copy(isSaving = false, sheetArgs = null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}