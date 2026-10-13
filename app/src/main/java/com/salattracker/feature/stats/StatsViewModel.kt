package com.salattracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StatusTrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── Period enum ───────────────────────────────────────────────────────────────

/**
 * Selectable stats range. [days] is the length of the range (inclusive of today),
 * and drives both the date-range math and the point/label resolution policy in the
 * repositories (daily ≤ 40, weekly 41–120, monthly > 120).
 */
enum class StatsPeriod(val days: Int, val displayLabel: String) {
    WEEK(7, "Week"),
    TWO_WEEKS(14, "2-Week"),
    THREE_WEEKS(21, "3-Week"),
    FOUR_WEEKS(28, "4-Week"),
    MONTH(30, "Month"),
    FORTY_DAYS(40, "40 Days"),
    NINETY_DAYS(90, "90 Days"),
    SIX_MONTHS(182, "6-Month"),
    YEAR(365, "Year"),
}

// ── UI state — each section owns its own period + loading + data ──────────────

data class MetricsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val isLoading: Boolean = true,
    val congregationPct: Int = 0,
    val individualPct: Int = 0,
    val missedPct: Int = 0,
    val congregationDelta: Int = 0,
)

data class StatusTrendUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val isLoading: Boolean = true,
    val points: List<StatusTrendPoint> = emptyList(),
)

data class GoalsBarUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val isLoading: Boolean = true,
    // Short ranges (days ≤ 28) show a per-day breakdown (dailyPoints); longer ranges show a per-goal breakdown (items).
    val items: List<GoalCompletionStat> = emptyList(),
    val dailyPoints: List<GoalDailyPoint> = emptyList(),
)

/** Best-ever streak for a single prayer, shown in the Highest Streaks card. */
data class PrayerHighestStreak(
    val prayerType: PrayerType,
    val highest: Int,
)

data class StatsUiState(
    val metrics: MetricsUiState = MetricsUiState(),
    val statusTrend: StatusTrendUiState = StatusTrendUiState(),
    val goalsBar: GoalsBarUiState = GoalsBarUiState(),

    // Not period-gated — live per-prayer streak snapshot.
    val currentStreak: Int = 0,
    val showAchievement: Boolean = false,
    val highestStreaks: List<PrayerHighestStreak> = emptyList(),

    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadMetrics(StatsPeriod.WEEK)
        loadStatusTrend(StatsPeriod.WEEK)
        loadGoalsBar(StatsPeriod.WEEK)
        observeStreaks()
        observeGoalChanges()
    }

    fun onStatusTrendPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(statusTrend = it.statusTrend.copy(period = period, isLoading = true)) }
        loadStatusTrend(period)
    }

    fun onGoalsBarPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(goalsBar = it.goalsBar.copy(period = period, isLoading = true)) }
        loadGoalsBar(period)
    }

    private fun loadMetrics(period: StatsPeriod) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                val stats = repository.getStatsForPeriod(from, to)
                val prevStats = repository.getStatsForPeriod(from.minusDays(period.days.toLong()), from.minusDays(1))
                stats to prevStats
            }.onSuccess { (stats, prevStats) ->
                _uiState.update {
                    it.copy(
                        metrics = it.metrics.copy(
                            isLoading         = false,
                            congregationPct   = stats.congregationPct,
                            individualPct     = stats.individualPct,
                            missedPct         = stats.missedPct,
                            congregationDelta = stats.congregationPct - prevStats.congregationPct,
                        )
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, metrics = it.metrics.copy(isLoading = false)) }
            }
        }
    }

    private fun loadStatusTrend(period: StatsPeriod) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                repository.getStatusTrendPoints(from, to, period.days)
            }.onSuccess { points ->
                _uiState.update { it.copy(statusTrend = it.statusTrend.copy(isLoading = false, points = points)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, statusTrend = it.statusTrend.copy(isLoading = false)) }
            }
        }
    }

    private fun loadGoalsBar(period: StatsPeriod) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                if (period.days <= 28) {
                    goalRepository.getDailyGoalCompletionPoints(from, to) to emptyList()
                } else {
                    emptyList<GoalDailyPoint>() to goalRepository.getGoalCompletionBreakdown(from, to)
                }
            }.onSuccess { (dailyPoints, items) ->
                _uiState.update { it.copy(goalsBar = it.goalsBar.copy(isLoading = false, dailyPoints = dailyPoints, items = items)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, goalsBar = it.goalsBar.copy(isLoading = false)) }
            }
        }
    }

    private fun observeStreaks() {
        viewModelScope.launch {
            // Live so the highest streaks stay fresh when prayers are marked from another screen.
            repository.observeStreaks().collect { snapshot ->
                // The combined "current streak" is the weakest link — days ALL 5 held.
                val current = snapshot.values.minOfOrNull { it.currentStreak } ?: 0
                val highest = PrayerType.values().map { PrayerHighestStreak(it, snapshot[it]?.highestStreak ?: 0) }
                _uiState.update {
                    it.copy(currentStreak = current, showAchievement = current >= 5, highestStreaks = highest)
                }
                // A prayer was marked somewhere (this screen or another) — keep the week's
                // metrics and trend chart in step. loadMetrics/loadStatusTrend never flip
                // isLoading themselves (only the explicit period-change handlers above do),
                // so this never flashes a skeleton over already-visible data.
                loadMetrics(_uiState.value.metrics.period)
                loadStatusTrend(_uiState.value.statusTrend.period)
            }
        }
    }

    private fun observeGoalChanges() {
        viewModelScope.launch {
            // Live so the goals chart stays fresh when a goal is added, checked in, or edited
            // from elsewhere — loadGoalsBar never flips isLoading itself either.
            goalRepository.observeActiveGoalsWithProgress().collect {
                loadGoalsBar(_uiState.value.goalsBar.period)
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

// ── Period helpers ────────────────────────────────────────────────────────────

private fun StatsPeriod.dateRange(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minusDays((days - 1).toLong()) to today
