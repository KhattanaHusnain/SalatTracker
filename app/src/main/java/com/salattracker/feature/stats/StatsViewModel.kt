package com.salattracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StreakPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── Period enum ───────────────────────────────────────────────────────────────

enum class StatsPeriod { WEEK, MONTH, YEAR }

// ── UI state — uses domain types directly (no duplicate wrappers) ─────────────

data class StatsUiState(
    val isLoading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.WEEK,

    val congregationPct: Int = 0,
    val individualPct: Int = 0,
    val missedPct: Int = 0,
    val congregationDelta: Int = 0,

    // StreakPoint and PrayerBreakdown are now domain types from PrayerRepository.kt
    val streakPoints: List<StreakPoint> = emptyList(),
    val prayerBreakdown: List<PrayerBreakdown> = emptyList(),

    val currentStreak: Int = 0,
    val showAchievement: Boolean = false,
    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { loadStats(StatsPeriod.WEEK) }

    fun onPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(period = period, isLoading = true) }
        loadStats(period)
    }

    private fun loadStats(period: StatsPeriod) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)

                val stats      = repository.getStatsForPeriod(from, to)
                val prevStats  = repository.getStatsForPeriod(
                    from.minus(period.javaPeriod), from.minusDays(1)
                )
                // Pass the period label string — repository uses it to format X-axis labels
                val points     = repository.getStreakPoints(from, to, period.name.lowercase())
                val breakdown  = repository.getPrayerBreakdown(from, to)
                val streak     = repository.getStreakSnapshot().first

                _uiState.update {
                    it.copy(
                        isLoading         = false,
                        congregationPct   = stats.congregationPct,
                        individualPct     = stats.individualPct,
                        missedPct         = stats.missedPct,
                        congregationDelta = stats.congregationPct - prevStats.congregationPct,
                        streakPoints      = points,
                        prayerBreakdown   = breakdown,
                        currentStreak     = streak,
                        showAchievement   = streak >= 5,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

// ── Period helpers ────────────────────────────────────────────────────────────

private fun StatsPeriod.dateRange(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    StatsPeriod.WEEK  -> today.minusDays(6) to today
    StatsPeriod.MONTH -> today.minusDays(29) to today
    StatsPeriod.YEAR  -> today.minusDays(364) to today
}

private val StatsPeriod.javaPeriod: java.time.Period get() = when (this) {
    StatsPeriod.WEEK  -> java.time.Period.ofWeeks(1)
    StatsPeriod.MONTH -> java.time.Period.ofDays(30)
    StatsPeriod.YEAR  -> java.time.Period.ofYears(1)
}