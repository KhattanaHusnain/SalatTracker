package com.salattracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.core.util.runCatchingCancellable
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.usecase.PerfectDays
import com.salattracker.domain.usecase.completionPercent
import com.salattracker.domain.usecase.completionTrend
import com.salattracker.domain.usecase.perfectDays
import com.salattracker.domain.usecase.trendBucketFor
import com.salattracker.domain.usecase.weekdayCompletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

// ── Period enum ───────────────────────────────────────────────────────────────

/**
 * The single range selector that drives every period-gated section of Stats. [days] is the
 * length of the range (inclusive of today), and drives both the date-range math and the
 * trend-bucketing policy in [com.salattracker.domain.usecase.trendBucketFor] (daily ≤ 40,
 * weekly ≤ 120, monthly beyond). Display labels live in StatsScreen.kt ([labelRes]) since this
 * is a domain-adjacent enum with no feature-layer imports, and localized strings need
 * @Composable / R access.
 */
enum class StatsPeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
    FORTY_DAYS(40),
    NINETY_DAYS(90),
    YEAR(365),
}

// ── UI state — one shared period, each section owns its own loading + data ────

/** One point of the completion-trend line — unlike the domain's [com.salattracker.domain.usecase.CompletionTrendPoint],
 * [completionPct] here is never null: empty buckets are already filtered out before this is built. */
data class TrendPoint(val start: LocalDate, val completionPct: Int)

data class OverviewUiState(
    val isLoading: Boolean = true,
    val counts: StatusCounts = StatusCounts(),
    val completionPct: Int? = null,
    /** Null when the previous period had nothing countable to compare against. */
    val completionDelta: Int? = null,
    val perfectDays: PerfectDays = PerfectDays(perfect = 0, counted = 0),
    val trend: List<TrendPoint> = emptyList(),
    /** All 7 keys always present; a weekday with no countable prayers maps to null. */
    val weekday: Map<DayOfWeek, Int?> = emptyMap(),
)

data class PrayerBreakdownUiState(
    val isLoading: Boolean = true,
    val breakdown: List<PrayerBreakdown> = emptyList(),
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val items: List<GoalPeriodStat> = emptyList(),
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.MONTH,

    val overview: OverviewUiState = OverviewUiState(),
    val prayerBreakdown: PrayerBreakdownUiState = PrayerBreakdownUiState(),
    val goals: GoalsUiState = GoalsUiState(),

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
    private var overviewJob: Job? = null
    private var breakdownJob: Job? = null
    private var goalsJob: Job? = null

    init {
        loadAllForPeriod(StatsPeriod.MONTH, showSkeleton = true)
        observePrayerChanges()
        observeGoalChanges()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(period = period) }
        loadAllForPeriod(period, showSkeleton = true)
    }

    private fun loadAllForPeriod(period: StatsPeriod, showSkeleton: Boolean) {
        loadOverview(period, showSkeleton)
        loadBreakdown(period, showSkeleton)
        loadGoals(period, showSkeleton)
    }

    private fun loadOverview(period: StatsPeriod, showSkeleton: Boolean) {
        overviewJob?.cancel()
        overviewJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(overview = it.overview.copy(isLoading = true)) }
            runCatchingCancellable {
                val today = LocalDate.now()
                val (from, to) = period.dateRange(today)
                val currentDays = repository.getDailyStatusCounts(from, to)
                val previousDays = repository.getDailyStatusCounts(from.minusDays(period.days.toLong()), from.minusDays(1))
                Triple(currentDays, previousDays, today)
            }.onSuccess { (currentDays, previousDays, today) ->
                val counts = currentDays.fold(StatusCounts()) { acc, d -> acc + d.counts }
                val prevCounts = previousDays.fold(StatusCounts()) { acc, d -> acc + d.counts }
                val pct = completionPercent(counts)
                val prevPct = completionPercent(prevCounts)
                val trend = completionTrend(currentDays, trendBucketFor(period.days), today)
                    .mapNotNull { point -> point.completionPct?.let { TrendPoint(point.start, it) } }
                _uiState.update {
                    it.copy(
                        overview = it.overview.copy(
                            isLoading       = false,
                            counts          = counts,
                            completionPct   = pct,
                            completionDelta = if (pct != null && prevPct != null) pct - prevPct else null,
                            perfectDays     = perfectDays(currentDays, today),
                            trend           = trend,
                            weekday         = weekdayCompletion(currentDays),
                        )
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, overview = it.overview.copy(isLoading = false)) }
            }
        }
    }

    private fun loadBreakdown(period: StatsPeriod, showSkeleton: Boolean) {
        breakdownJob?.cancel()
        breakdownJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(prayerBreakdown = it.prayerBreakdown.copy(isLoading = true)) }
            runCatchingCancellable {
                val (from, to) = period.dateRange(LocalDate.now())
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
            runCatchingCancellable {
                val (from, to) = period.dateRange(LocalDate.now())
                goalRepository.getGoalPeriodStats(from, to)
            }.onSuccess { items ->
                _uiState.update { it.copy(goals = it.goals.copy(isLoading = false, items = items)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, goals = it.goals.copy(isLoading = false)) }
            }
        }
    }

    private fun observePrayerChanges() {
        viewModelScope.launch {
            // observeStreaks() re-emits on every prayer write (and after a backup restore) from
            // any screen — it's used here purely as a change signal, not for its streak payload,
            // to keep the Overview and By Prayer cards fresh without flashing a skeleton over
            // already-visible data.
            repository.observeStreaks().collect {
                loadOverview(_uiState.value.period, showSkeleton = false)
                loadBreakdown(_uiState.value.period, showSkeleton = false)
            }
        }
    }

    private fun observeGoalChanges() {
        viewModelScope.launch {
            // Live so the goals card stays fresh when a goal is added, checked in, or edited from
            // elsewhere — no skeleton flash either.
            goalRepository.observeGoalHistories().collect {
                loadGoals(_uiState.value.period, showSkeleton = false)
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

// ── Period helpers ────────────────────────────────────────────────────────────

private fun StatsPeriod.dateRange(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minusDays((days - 1).toLong()) to today
