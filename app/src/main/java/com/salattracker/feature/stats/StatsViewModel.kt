package com.salattracker.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.core.util.runCatchingCancellable
import com.salattracker.domain.model.Gender
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.usecase.PerfectDays
import com.salattracker.domain.usecase.CongregationChallenge
import com.salattracker.domain.usecase.congregationChallenge
import com.salattracker.domain.usecase.congregationPercent
import com.salattracker.domain.usecase.completionPercent
import com.salattracker.domain.usecase.completionTrend
import com.salattracker.domain.usecase.perfectDays
import com.salattracker.domain.usecase.trendBucketFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

// ── Period enum ───────────────────────────────────────────────────────────────

/**
 * The single range selector that drives every period-gated section of Stats. [days] is the
 * length of the range (inclusive of today), and drives both the date-range math and the
 * trend-bucketing policy in [com.salattracker.domain.usecase.trendBucketFor]. Display labels live in StatsScreen.kt ([labelRes]) since this
 * is a domain-adjacent enum with no feature-layer imports, and localized strings need
 * @Composable / R access.
 */
enum class StatsPeriod(val days: Int) {
    WEEK(7),
    MONTH(30),
    NINETY_DAYS(90),
}

// ── UI state — one shared period, each section owns its own loading + data ────

/** One point of the completion-trend line. A null [completionPct] is a real time gap, not 0%. */
data class TrendPoint(val start: LocalDate, val completionPct: Int?)

data class OverviewUiState(
    val isLoading: Boolean = true,
    val counts: StatusCounts = StatusCounts(),
    val completionPct: Int? = null,
    /** Null when the previous period had nothing countable to compare against. */
    val completionDelta: Int? = null,
    val perfectDays: PerfectDays = PerfectDays(perfect = 0, counted = 0),
    val congregationPercent: Int? = null,
    val congregationDelta: Int? = null,
    val congregationChallenge: CongregationChallenge = CongregationChallenge(0, 0),
    val trend: List<TrendPoint> = emptyList(),
)

data class PrayerBreakdownUiState(
    val isLoading: Boolean = true,
    val breakdown: List<PrayerBreakdown> = emptyList(),
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.MONTH,

    val overview: OverviewUiState = OverviewUiState(),
    val prayerBreakdown: PrayerBreakdownUiState = PrayerBreakdownUiState(),
    val gender: Gender? = null,

    val errorRes: Int? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    // One Job per section so a fast period switch cancels an in-flight load for the old period
    // instead of risking it landing after (and overwriting) the new period's result.
    private var overviewJob: Job? = null
    private var breakdownJob: Job? = null

    private val dayTicker: Flow<LocalDate> = flow {
        while (true) {
            val today = LocalDate.now(clock)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
            delay((nextMidnight - clock.millis()).coerceAtLeast(1L))
        }
    }

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { prefs -> _uiState.update { it.copy(gender = prefs.gender) } }
        }
        loadAllForPeriod(StatsPeriod.MONTH, showSkeleton = true)
        observeDayChanges()
        observePrayerChanges()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        _uiState.update { it.copy(period = period) }
        loadAllForPeriod(period, showSkeleton = true)
    }

    private fun loadAllForPeriod(period: StatsPeriod, showSkeleton: Boolean) {
        loadOverview(period, showSkeleton)
        loadBreakdown(period, showSkeleton)
    }

    private fun loadOverview(period: StatsPeriod, showSkeleton: Boolean) {
        overviewJob?.cancel()
        overviewJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(overview = it.overview.copy(isLoading = true)) }
            runCatchingCancellable {
                val today = LocalDate.now(clock)
                val (from, to) = period.dateRange(today)
                val currentDays = repository.getDailyStatusCounts(from, to)
                val previousDays = repository.getDailyStatusCounts(from.minusDays(period.days.toLong()), from.minusDays(1))
                val challengeDays = repository.getDailyStatusCounts(today.minusDays(40), today.minusDays(1))
                OverviewLoad(currentDays, previousDays, challengeDays, from, to)
            }.onSuccess { (currentDays, previousDays, challengeDays, from, to) ->
                val counts = currentDays.fold(StatusCounts()) { acc, d -> acc + d.counts }
                val prevCounts = previousDays.fold(StatusCounts()) { acc, d -> acc + d.counts }
                val pct = completionPercent(counts)
                val prevPct = completionPercent(prevCounts)
                val congregationPct = congregationPercent(counts)
                val previousCongregationPct = congregationPercent(prevCounts)
                val trend = completionTrend(currentDays, trendBucketFor(period.days), from, to)
                    .map { point -> TrendPoint(point.start, point.completionPct) }
                _uiState.update {
                    it.copy(
                        overview = it.overview.copy(
                            isLoading       = false,
                            counts          = counts,
                            completionPct   = pct,
                            completionDelta = if (pct != null && prevPct != null) pct - prevPct else null,
                            perfectDays     = perfectDays(currentDays, to),
                            congregationPercent = congregationPct,
                            congregationDelta = if (congregationPct != null && previousCongregationPct != null) congregationPct - previousCongregationPct else null,
                            congregationChallenge = congregationChallenge(challengeDays, to),
                            trend           = trend,
                        )
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(errorRes = R.string.error_load_generic, overview = it.overview.copy(isLoading = false)) }
            }
        }
    }

    private fun loadBreakdown(period: StatsPeriod, showSkeleton: Boolean) {
        breakdownJob?.cancel()
        breakdownJob = viewModelScope.launch {
            if (showSkeleton) _uiState.update { it.copy(prayerBreakdown = it.prayerBreakdown.copy(isLoading = true)) }
            runCatchingCancellable {
                val (from, to) = period.dateRange(LocalDate.now(clock))
                repository.getPrayerBreakdown(from, to)
            }.onSuccess { breakdown ->
                _uiState.update { it.copy(prayerBreakdown = it.prayerBreakdown.copy(isLoading = false, breakdown = breakdown)) }
            }.onFailure {
                _uiState.update { it.copy(errorRes = R.string.error_load_generic, prayerBreakdown = it.prayerBreakdown.copy(isLoading = false)) }
            }
        }
    }

    private fun observePrayerChanges() {
        viewModelScope.launch {
            // observeStreaks() re-emits on every prayer write (and after a backup restore) from
            // any screen — it's used here purely as a change signal, not for its streak payload,
            // to keep the Overview and By Prayer cards fresh without flashing a skeleton over
            // already-visible data.
            repository.observeStreaks().drop(1).collect {
                loadOverview(_uiState.value.period, showSkeleton = false)
                loadBreakdown(_uiState.value.period, showSkeleton = false)
            }
        }
    }

    private fun observeDayChanges() {
        viewModelScope.launch {
            dayTicker.drop(1).collect {
                loadAllForPeriod(_uiState.value.period, showSkeleton = true)
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}

private data class OverviewLoad(
    val currentDays: List<com.salattracker.domain.repository.DayStatusCounts>,
    val previousDays: List<com.salattracker.domain.repository.DayStatusCounts>,
    val challengeDays: List<com.salattracker.domain.repository.DayStatusCounts>,
    val from: LocalDate,
    val to: LocalDate,
)

// ── Period helpers ────────────────────────────────────────────────────────────

private fun StatsPeriod.dateRange(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minusDays((days - 1).toLong()) to today
