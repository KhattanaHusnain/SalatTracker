package com.salattracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StatusTrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── Period enum ───────────────────────────────────────────────────────────────

/**
 * The single range selector that drives every period-gated section of Stats. [days] is the
 * length of the range (inclusive of today), and drives both the date-range math and the
 * point/label resolution policy in the repositories (daily ≤ 40, weekly 41–120, monthly > 120).
 * Display labels live in StatsScreen.kt ([labelRes]) since this is a domain-adjacent enum with
 * no feature-layer imports, and localized strings need @Composable / R access.
 */
enum class StatsPeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
    FORTY_DAYS(40),
    NINETY_DAYS(90),
    YEAR(365),
}

// ── UI state — one shared period, each section owns its own loading + data ────

data class SummaryUiState(
    val isLoading: Boolean = true,
    val completionPct: Int = 0,
    val completionDelta: Int = 0,
    val congregationPct: Int = 0,
    val individualPct: Int = 0,
    val missedPct: Int = 0,
)

data class StatusMixUiState(
    val isLoading: Boolean = true,
    val points: List<StatusTrendPoint> = emptyList(),
)

data class PrayerBreakdownUiState(
    val isLoading: Boolean = true,
    val breakdown: List<PrayerBreakdown> = emptyList(),
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    // Short ranges (days ≤ 40) show a per-day breakdown (dailyPoints); longer ranges show a per-goal breakdown (items).
    val items: List<GoalCompletionStat> = emptyList(),
    val dailyPoints: List<GoalDailyPoint> = emptyList(),
)

/** Best-ever streak for a single prayer, shown in Streak Records. */
data class PrayerHighestStreak(
    val prayerType: PrayerType,
    val highest: Int,
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,

    val summary: SummaryUiState = SummaryUiState(),
    val statusMix: StatusMixUiState = StatusMixUiState(),
    val prayerBreakdown: PrayerBreakdownUiState = PrayerBreakdownUiState(),
    val goals: GoalsUiState = GoalsUiState(),

    // Not period-gated — live per-prayer streak snapshot, shown in Streak Records.
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

    // One Job per section so a fast period switch cancels an in-flight load for the old period
    // instead of risking it landing after (and overwriting) the new period's result.
    private var summaryJob: Job? = null
    private var statusMixJob: Job? = null
    private var breakdownJob: Job? = null
    private var goalsJob: Job? = null

    init {
        loadAllForPeriod(StatsPeriod.WEEK, showSkeleton = true)
        observeStreaks()
        observeGoalChanges()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(period = period) }
        loadAllForPeriod(period, showSkeleton = true)
    }

    private fun loadAllForPeriod(period: StatsPeriod, showSkeleton: Boolean) {
        loadSummary(period, showSkeleton)
        loadStatusMix(period, showSkeleton)
        loadBreakdown(period, showSkeleton)
        loadGoals(period, showSkeleton)
    }

    private fun loadSummary(period: StatsPeriod, showSkeleton: Boolean) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(summary = it.summary.copy(isLoading = true)) }
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                val stats = repository.getStatsForPeriod(from, to)
                val prevStats = repository.getStatsForPeriod(from.minusDays(period.days.toLong()), from.minusDays(1))
                stats to prevStats
            }.onSuccess { (stats, prevStats) ->
                _uiState.update {
                    it.copy(
                        summary = it.summary.copy(
                            isLoading         = false,
                            completionPct     = stats.completionPct,
                            completionDelta   = stats.completionPct - prevStats.completionPct,
                            congregationPct   = stats.congregationPct,
                            individualPct     = stats.individualPct,
                            missedPct         = stats.missedPct,
                        )
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, summary = it.summary.copy(isLoading = false)) }
            }
        }
    }

    private fun loadStatusMix(period: StatsPeriod, showSkeleton: Boolean) {
        statusMixJob?.cancel()
        statusMixJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(statusMix = it.statusMix.copy(isLoading = true)) }
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                repository.getStatusTrendPoints(from, to, period.days)
            }.onSuccess { points ->
                _uiState.update { it.copy(statusMix = it.statusMix.copy(isLoading = false, points = points)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, statusMix = it.statusMix.copy(isLoading = false)) }
            }
        }
    }

    private fun loadBreakdown(period: StatsPeriod, showSkeleton: Boolean) {
        breakdownJob?.cancel()
        breakdownJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(prayerBreakdown = it.prayerBreakdown.copy(isLoading = true)) }
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                repository.getPrayerBreakdown(from, to)
            }.onSuccess { breakdown ->
                _uiState.update { it.copy(prayerBreakdown = it.prayerBreakdown.copy(isLoading = false, breakdown = breakdown)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, prayerBreakdown = it.prayerBreakdown.copy(isLoading = false)) }
            }
        }
    }

    private fun loadGoals(period: StatsPeriod, showSkeleton: Boolean) {
        goalsJob?.cancel()
        goalsJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(goals = it.goals.copy(isLoading = true)) }
            runCatching {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                if (period.days <= 40) {
                    goalRepository.getDailyGoalCompletionPoints(from, to) to emptyList()
                } else {
                    emptyList<GoalDailyPoint>() to goalRepository.getGoalCompletionBreakdown(from, to)
                }
            }.onSuccess { (dailyPoints, items) ->
                _uiState.update { it.copy(goals = it.goals.copy(isLoading = false, dailyPoints = dailyPoints, items = items)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, goals = it.goals.copy(isLoading = false)) }
            }
        }
    }

    private fun observeStreaks() {
        viewModelScope.launch {
            // Live so Streak Records stays fresh when prayers are marked from another screen.
            repository.observeStreaks().collect { snapshot ->
                // The combined "current streak" is the weakest link — days ALL 5 held.
                val current = snapshot.values.minOfOrNull { it.currentStreak } ?: 0
                val highest = PrayerType.values().map { PrayerHighestStreak(it, snapshot[it]?.highestStreak ?: 0) }
                _uiState.update {
                    it.copy(currentStreak = current, showAchievement = current >= 5, highestStreaks = highest)
                }
                // A prayer was marked somewhere (this screen or another) — keep summary, status
                // mix and the per-prayer breakdown in step, without flashing a skeleton over
                // already-visible data.
                loadSummary(_uiState.value.period, showSkeleton = false)
                loadStatusMix(_uiState.value.period, showSkeleton = false)
                loadBreakdown(_uiState.value.period, showSkeleton = false)
            }
        }
    }

    private fun observeGoalChanges() {
        viewModelScope.launch {
            // Live so the goals chart stays fresh when a goal is added, checked in, or edited
            // from elsewhere — no skeleton flash either.
            goalRepository.observeActiveGoalsWithProgress().collect {
                loadGoals(_uiState.value.period, showSkeleton = false)
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

// ── Period helpers ────────────────────────────────────────────────────────────

private fun StatsPeriod.dateRange(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minusDays((days - 1).toLong()) to today
