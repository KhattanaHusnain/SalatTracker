package com.salattracker.feature.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.ShimmerBox
import com.salattracker.core.ui.StatTile
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalPeriodStat
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.usecase.PerfectDays
import com.salattracker.domain.usecase.allPrayersOnTrack
import com.salattracker.domain.usecase.trendBucketFor
import com.salattracker.domain.usecase.weakestPrayer
import com.salattracker.domain.usecase.weakestWeekday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

// ── Stats Screen ─────────────────────────────────────────────────────────────
// A today-vs-history dashboard: one shared period selector drives everything except Goals
// (judged by each goal's own frequency, not the period) and By Weekday (30D+ only). Order:
// summary -> trend -> by prayer -> by weekday -> goals, reading as "where do I stand" before
// "what's my long-run pattern".

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onPeriodChanged: (StatsPeriod) -> Unit,
    onAddGoalClick: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            PeriodToggle(selected = uiState.period, onSelect = onPeriodChanged)
            SummaryCard(state = uiState.overview)
            TrendCard(state = uiState.overview, period = uiState.period)
            PrayerBreakdownCard(state = uiState.prayerBreakdown)
            if (uiState.period.days >= 30) {
                WeekdayCard(weekday = uiState.overview.weekday, isLoading = uiState.overview.isLoading)
            }
            GoalsCard(state = uiState.goals, onAddGoalClick = onAddGoalClick)
        }
        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }
    }
}

// ── Shared period selector — one row drives every period-gated section ──────────

@get:androidx.annotation.StringRes
private val StatsPeriod.labelRes: Int get() = when (this) {
    StatsPeriod.WEEK        -> R.string.stats_period_7d
    StatsPeriod.MONTH       -> R.string.stats_period_30d
    StatsPeriod.FORTY_DAYS  -> R.string.stats_period_40d
    StatsPeriod.NINETY_DAYS -> R.string.stats_period_90d
    StatsPeriod.YEAR        -> R.string.stats_period_1y
}

@Composable
private fun PeriodToggle(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsPeriod.values().forEach { period ->
            PeriodChip(
                label      = stringResource(period.labelRes),
                isSelected = selected == period,
                onClick    = { onSelect(period) },
            )
        }
    }
}

@Composable
private fun PeriodChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (isSelected) SalatPrimaryContainer else SalatSurfaceContainerLow,
        border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color      = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
            maxLines   = 1,
        )
    }
}

// ── Shared card chrome / loading / empty state ──────────────────────────────────

@Composable
private fun StatsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurface,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun EmptyStateText(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily, textAlign = TextAlign.Center)
    }
}

// ── Shared stacked status bar — congregation / individual / qaza done / qaza pending,
// excused prayers left out so the bar's width always matches the completion % it sits under.

@Composable
private fun StatusBar(counts: StatusCounts, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    if (counts.countable == 0) {
        Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(SalatSurfaceVariant))
        return
    }
    val animCongregation by animateFloatAsState(counts.congregation.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "c")
    val animIndividual by animateFloatAsState(counts.individual.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "i")
    val animQazaCompleted by animateFloatAsState(counts.qazaCompleted.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "qc")
    val animQazaPending by animateFloatAsState(counts.qazaPending.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "qp")

    Row(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))) {
        if (animCongregation > 0f) Box(Modifier.weight(animCongregation).fillMaxHeight().background(SalatStatusCongregation))
        if (animIndividual > 0f) Box(Modifier.weight(animIndividual).fillMaxHeight().background(SalatStatusIndividual))
        if (animQazaCompleted > 0f) Box(Modifier.weight(animQazaCompleted).fillMaxHeight().background(SalatStatusQazaCompleted))
        if (animQazaPending > 0f) Box(Modifier.weight(animQazaPending).fillMaxHeight().background(SalatStatusQazaPending))
    }
}

@Composable
private fun LegendCount(color: Color, label: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text("$label: $count", color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Summary (prayed on time %, status bar + legend, perfect days, qaza recovery) ─

@Composable
private fun SummaryCard(state: OverviewUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_prayed_on_time), color = SalatOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = InterFamily, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        if (state.isLoading) {
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(36.dp))
            Spacer(Modifier.height(16.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(10.dp))
            return@StatsCard
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = state.completionPct?.let { "$it%" } ?: "—",
                color = SalatPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily,
            )
            state.completionDelta?.let { delta ->
                Text(
                    text = stringResource(R.string.stats_vs_last, "${if (delta > 0) "+" else ""}$delta"),
                    color = if (delta > 0) SalatSurfaceTint else SalatError,
                    fontSize = 11.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        if (state.completionPct == null) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.stats_no_data_period), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
        }
        Spacer(Modifier.height(16.dp))
        StatusBar(state.counts)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendCount(SalatStatusCongregation, stringResource(R.string.status_congregation), state.counts.congregation, Modifier.weight(1f))
                LegendCount(SalatStatusIndividual, stringResource(R.string.status_individual), state.counts.individual, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendCount(SalatStatusQazaCompleted, stringResource(R.string.status_qaza_done), state.counts.qazaCompleted, Modifier.weight(1f))
                LegendCount(SalatStatusQazaPending, stringResource(R.string.status_qaza), state.counts.qazaPending, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.stats_perfect_days),
                value = if (state.perfectDays.counted > 0) "${state.perfectDays.perfect}/${state.perfectDays.counted}" else "—",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.stats_qaza_made_up),
                value = if (state.counts.missed > 0) "${state.counts.qazaCompleted}/${state.counts.missed}" else stringResource(R.string.stats_nothing_missed),
                modifier = Modifier.weight(1f),
            )
        }
        if (state.counts.excused > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.stats_excused_not_counted, state.counts.excused),
                color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily,
            )
        }
    }
}

// ── Trend (Vico line, 0-100, no scroll) ──────────────────────────────────────────

@Composable
private fun TrendCard(state: OverviewUiState, period: StatsPeriod) {
    StatsCard {
        Text(stringResource(R.string.stats_trend), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> ShimmerBox(Modifier.fillMaxWidth().height(180.dp))
            state.trend.size < 2 -> EmptyStateText(stringResource(R.string.stats_not_enough_data))
            else -> CompletionTrendChart(state.trend, trendBucketFor(period.days), Modifier.fillMaxWidth())
        }
    }
}

// ── By prayer (stacked status bar per prayer) ────────────────────────────────────

@Composable
private fun PrayerBreakdownCard(state: PrayerBreakdownUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_by_prayer), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(20.dp)) }
            }
            state.breakdown.isEmpty() -> EmptyStateText(stringResource(R.string.stats_no_data_period))
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.breakdown.forEach { entry -> PrayerBarRow(entry) }
                }
                Spacer(Modifier.height(12.dp))
                val weakest = remember(state.breakdown) { weakestPrayer(state.breakdown) }
                val allOnTrack = remember(state.breakdown) { allPrayersOnTrack(state.breakdown) }
                if (weakest != null) {
                    Text(
                        text = stringResource(R.string.stats_weakest_prayer, weakest.prayerType.shortLabel()),
                        color = SalatError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
                    )
                } else if (allOnTrack) {
                    Text(
                        text = stringResource(R.string.stats_all_on_track),
                        color = SalatSurfaceTint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerBarRow(entry: PrayerBreakdown, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(entry.prayerType.iconRes), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.prayerType.shortLabel(), color = SalatOnSurface, fontSize = 13.sp, fontFamily = InterFamily,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) { StatusBar(entry.counts, height = 8.dp) }
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.completionRate?.let { "${(it * 100).toInt()}%" } ?: "—",
            color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily, textAlign = TextAlign.End, modifier = Modifier.width(36.dp),
        )
    }
}

// ── By weekday (30D+ only) ────────────────────────────────────────────────────────

private val weekOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
private fun WeekdayCard(weekday: Map<DayOfWeek, Int?>, isLoading: Boolean) {
    StatsCard {
        Text(stringResource(R.string.stats_by_weekday), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            ShimmerBox(Modifier.fillMaxWidth().height(96.dp))
            return@StatsCard
        }
        val weakest = remember(weekday) { weakestWeekday(weekday) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekOrder.forEach { day ->
                WeekdayBar(day = day, pct = weekday[day], isWeakest = day == weakest, modifier = Modifier.weight(1f))
            }
        }
        if (weakest != null) {
            Spacer(Modifier.height(14.dp))
            val locale = LocalConfiguration.current.locales[0]
            val fullName = remember(weakest, locale) { weakest.getDisplayName(TextStyle.FULL, locale) }
            Text(
                text = stringResource(R.string.stats_weakest_day, fullName),
                color = SalatError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
            )
        }
    }
}

@Composable
private fun WeekdayBar(day: DayOfWeek, pct: Int?, isWeakest: Boolean, modifier: Modifier = Modifier) {
    val fraction = ((pct ?: 0) / 100f).coerceIn(0.04f, 1f)
    val animFraction by animateFloatAsState(fraction, tween(800, easing = FastOutSlowInEasing), label = "wd")
    val color = when {
        pct == null -> SalatSurfaceVariant
        isWeakest   -> SalatError
        else        -> SalatPrimaryContainer
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.width(18.dp).fillMaxHeight(animFraction).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Spacer(Modifier.height(6.dp))
        Text(day.shortLabel(), color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, maxLines = 1)
    }
}

// ── Goals (per-goal periods hit, judged by each goal's own frequency) ───────────────

@Composable
private fun GoalsCard(state: GoalsUiState, onAddGoalClick: () -> Unit) {
    StatsCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.nav_goals), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
            Text(stringResource(R.string.stats_targets_hit), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) { ShimmerBox(Modifier.fillMaxWidth().height(40.dp)) }
            }
            state.items.isEmpty() -> EmptyStateText(stringResource(R.string.stats_no_active_goals), onClick = onAddGoalClick)
            else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.items.forEach { item -> GoalStatRow(item) }
            }
        }
    }
}

@Composable
private fun GoalStatRow(item: GoalPeriodStat) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(SalatPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(item.category.iconRes), null, tint = SalatOnPrimaryContainer, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.goalName.shortLabel(), color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${item.loggedAmount} ${goalUnitLabel(item.unit)} · ${item.frequency.label()}",
                    color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (item.periodsCounted > 0) "${item.periodsHit}/${item.periodsCounted}" else "—",
                color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
            )
        }
        Spacer(Modifier.height(8.dp))
        val rate = if (item.periodsCounted > 0) item.periodsHit.toFloat() / item.periodsCounted else 0f
        LinearProgressBar(rate, SalatPrimaryContainer, SalatSurfaceContainerHigh, 8.dp)
    }
}

@Composable
private fun LinearProgressBar(progress: Float, color: Color, trackColor: Color, height: Dp = 6.dp) {
    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "p")
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(color))
    }
}

private fun String.shortLabel(): String = if (length <= 14) this else take(13) + "…"

// ── Preview ───────────────────────────────────────────────────────────────────

private fun sampleBreakdown(rates: List<Float>): List<PrayerBreakdown> =
    PrayerType.values().mapIndexed { i, type ->
        val prayed = (rates[i] * 20).toInt()
        PrayerBreakdown(type, StatusCounts(congregation = prayed, individual = 0, qazaPending = 20 - prayed))
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen(
        uiState = StatsUiState(
            period = StatsPeriod.MONTH,
            overview = OverviewUiState(
                isLoading = false,
                counts = StatusCounts(congregation = 18, individual = 7, qazaCompleted = 3, qazaPending = 2, excused = 0),
                completionPct = 83, completionDelta = 5,
                perfectDays = PerfectDays(perfect = 18, counted = 26),
                trend = (0..9).map { TrendPoint(LocalDate.now().minusDays((9 - it).toLong()), listOf(60,65,70,68,75,80,78,85,88,90)[it]) },
                weekday = weekOrder.associateWith { listOf(80, 70, 90, 60, 75, 85, 95)[weekOrder.indexOf(it)] },
            ),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = sampleBreakdown(listOf(0.6f, 0.9f, 0.75f, 1f, 0.95f))),
            goals = GoalsUiState(
                isLoading = false,
                items = listOf(
                    GoalPeriodStat("Tahajjud Prayer", GoalCategory.SALAT, GoalFrequency.DAILY, "Time(s)", loggedAmount = 18, periodsHit = 18, periodsCounted = 26),
                    GoalPeriodStat("Read Quran", GoalCategory.QURAN, GoalFrequency.WEEKLY, "Pages", loggedAmount = 142, periodsHit = 2, periodsCounted = 4),
                    GoalPeriodStat("Morning Adhkar", GoalCategory.DHIKR, GoalFrequency.DAILY, "Time(s)", loggedAmount = 26, periodsHit = 26, periodsCounted = 26),
                ),
            ),
        ),
        onPeriodChanged = {},
        onAddGoalClick  = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenNinetyDaysPreview() {
    StatsScreen(
        uiState = StatsUiState(
            period = StatsPeriod.NINETY_DAYS,
            overview = OverviewUiState(
                isLoading = false,
                counts = StatusCounts(congregation = 150, individual = 90, qazaCompleted = 10, qazaPending = 15, excused = 5),
                completionPct = 68, completionDelta = -3,
                perfectDays = PerfectDays(perfect = 40, counted = 90),
                trend = (0..12).map { TrendPoint(LocalDate.now().minusDays(((12 - it) * 7).toLong()), (50..90).shuffled().first()) },
                weekday = weekOrder.associateWith { listOf(40, 80, 70, 90, 85, 60, 75)[weekOrder.indexOf(it)] },
            ),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = sampleBreakdown(listOf(0.4f, 0.8f, 0.7f, 0.9f, 0.85f))),
            goals = GoalsUiState(
                isLoading = false,
                items = listOf(
                    GoalPeriodStat("Tahajjud Prayer", GoalCategory.SALAT, GoalFrequency.DAILY, "Time(s)", loggedAmount = 60, periodsHit = 60, periodsCounted = 90),
                    GoalPeriodStat("Read Quran", GoalCategory.QURAN, GoalFrequency.MONTHLY, "Pages", loggedAmount = 210, periodsHit = 1, periodsCounted = 3),
                ),
            ),
        ),
        onPeriodChanged = {},
        onAddGoalClick  = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenEmptyPreview() {
    StatsScreen(
        uiState = StatsUiState(
            period = StatsPeriod.MONTH,
            overview = OverviewUiState(isLoading = false),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = emptyList()),
            goals = GoalsUiState(isLoading = false),
        ),
        onPeriodChanged = {},
        onAddGoalClick  = {},
    )
}
