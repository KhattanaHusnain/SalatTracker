package com.salattracker.feature.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.ShimmerBox
import com.salattracker.core.ui.StatTile
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.isOfferedFor
import com.salattracker.core.ui.label
import com.salattracker.core.ui.labelRes
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.Gender
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusCounts
import com.salattracker.domain.usecase.PerfectDays
import com.salattracker.domain.usecase.allPrayersOnTrack
import com.salattracker.domain.usecase.trendBucketFor
import com.salattracker.domain.usecase.weakestPrayer
import com.salattracker.domain.usecase.StatsInsight
import com.salattracker.domain.usecase.statsInsight
import java.time.LocalDate
import java.text.NumberFormat

// ── Stats Screen ─────────────────────────────────────────────────────────────
// A today-vs-history dashboard: one shared period selector drives summary, trend, and prayer
// breakdown, reading as "where do I stand" before "what should I improve".

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onPeriodChanged: (StatsPeriod) -> Unit,
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
            StatsInsightRow(uiState.overview.completionDelta)
            SummaryCard(state = uiState.overview, gender = uiState.gender)
            if (uiState.gender != Gender.FEMALE) {
                CongregationCard(state = uiState.overview, breakdown = uiState.prayerBreakdown)
            }
            uiState.overview.qazaRecovery?.let { QazaRecoveryCard(it) }
            TrendCard(state = uiState.overview, period = uiState.period)
            PrayerBreakdownCard(state = uiState.prayerBreakdown)
        }
        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
        }
    }
}

@Composable
private fun StatsInsightRow(completionDelta: Int?) {
    val insight = statsInsight(completionDelta) ?: return
    val locale = LocalConfiguration.current.locales[0]
    val points = (insight as StatsInsight.CompletionChanged).points
    val text = if (points > 0) {
        stringResource(R.string.stats_insight_improved, formatStatsPercent(points, locale))
    } else {
        stringResource(R.string.stats_insight_declined, formatStatsPercent(-points, locale))
    }
    Surface(color = SalatPrimaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = SalatOnPrimaryContainer, fontSize = 13.sp, fontFamily = AppUiFontFamily, modifier = Modifier.padding(12.dp))
    }
}

// ── Shared period selector — one row drives every period-gated section ──────────

@get:androidx.annotation.StringRes
private val StatsPeriod.labelRes: Int get() = when (this) {
    StatsPeriod.WEEK        -> R.string.stats_period_7d
    StatsPeriod.MONTH       -> R.string.stats_period_30d
    StatsPeriod.NINETY_DAYS -> R.string.stats_period_90d
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
            fontFamily = AppUiFontFamily,
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
        Text(text, color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center)
    }
}

// ── Shared stacked status bar — congregation / individual / qaza done / qaza pending,
// excused prayers left out so the bar's width always matches the completion % it sits under.

@Composable
private fun StatusBar(counts: StatusCounts, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val locale = LocalConfiguration.current.locales[0]
    val statusDescription = listOf(
        "${stringResource(R.string.status_congregation)}: ${formatStatsNumber(counts.congregation, locale)}",
        "${stringResource(R.string.status_individual)}: ${formatStatsNumber(counts.individual, locale)}",
        "${stringResource(PrayerStatus.QAZA_COMPLETED.labelRes)}: ${formatStatsNumber(counts.qazaCompleted, locale)}",
        "${stringResource(PrayerStatus.QAZA_PENDING.labelRes)}: ${formatStatsNumber(counts.qazaPending, locale)}",
    ).joinToString()
    val noDataDescription = stringResource(R.string.stats_no_data_period)
    if (counts.countable == 0) {
        Box(modifier.semantics { contentDescription = noDataDescription }.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(SalatSurfaceVariant))
        return
    }
    val animCongregation by animateFloatAsState(counts.congregation.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "c")
    val animIndividual by animateFloatAsState(counts.individual.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "i")
    val animQazaCompleted by animateFloatAsState(counts.qazaCompleted.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "qc")
    val animQazaPending by animateFloatAsState(counts.qazaPending.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "qp")

    Row(modifier.semantics { contentDescription = statusDescription }.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))) {
        if (animCongregation > 0f) Box(Modifier.weight(animCongregation).fillMaxHeight().background(SalatStatusCongregation))
        if (animIndividual > 0f) Box(Modifier.weight(animIndividual).fillMaxHeight().background(SalatStatusIndividual))
        if (animQazaCompleted > 0f) Box(Modifier.weight(animQazaCompleted).fillMaxHeight().background(SalatStatusQazaCompleted))
        if (animQazaPending > 0f) Box(Modifier.weight(animQazaPending).fillMaxHeight().background(SalatStatusQazaPending))
    }
}

// ── Summary (completed-prayer %, status bar + legend, perfect days, qaza recovery) ─

@Composable
private fun SummaryCard(state: OverviewUiState, gender: Gender?) {
    val locale = LocalConfiguration.current.locales[0]
    StatsCard {
        Text(stringResource(R.string.stats_prayed_on_time), color = SalatOnSurfaceVariant,  fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        if (state.isLoading) {
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(36.dp))
            Spacer(Modifier.height(16.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(10.dp))
            return@StatsCard
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = state.completionPct?.let { formatStatsPercent(it, locale) } ?: "—",
                color = SalatPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily,
            )
            state.completionDelta?.let { delta ->
                Text(
                    text = when {
                        delta > 0 -> stringResource(R.string.stats_vs_last, "+${formatStatsPercent(delta, locale)}")
                        delta < 0 -> stringResource(R.string.stats_vs_last, "−${formatStatsPercent(-delta, locale)}")
                        else -> stringResource(R.string.stats_no_change)
                    },
                    color = when {
                        delta > 0 -> SalatSurfaceTint
                        delta < 0 -> SalatError
                        else -> SalatOnSurfaceVariant
                    },
                     fontSize = 12.sp, fontFamily = AppUiFontFamily, modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        if (state.completionPct == null) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.stats_no_data_period), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
        state.loggingCoverage.percent?.let { coverage ->
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.stats_coverage, state.loggingCoverage.logged, state.loggingCoverage.expected), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            if (coverage < 70) Text(stringResource(R.string.stats_coverage_low), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
        Spacer(Modifier.height(16.dp))
        StatusBar(state.counts)
        Spacer(Modifier.height(16.dp))
        StatTile(
            label = stringResource(R.string.stats_perfect_days),
            value = if (state.perfectDays.counted > 0) formatStatsRatio(state.perfectDays.perfect, state.perfectDays.counted, locale) else "—",
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.counts.excused > 0 || PrayerStatus.MENSTRUATION.isOfferedFor(gender)) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.stats_excused_not_counted, state.counts.excused),
                color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
    }
}

@Composable
private fun CongregationCard(state: OverviewUiState, breakdown: PrayerBreakdownUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_congregation), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        Spacer(Modifier.height(12.dp))
        if (state.isLoading) {
            ShimmerBox(Modifier.fillMaxWidth().height(88.dp))
            return@StatsCard
        }
        val locale = LocalConfiguration.current.locales[0]
        val rate = state.congregationPercent
        if (rate == null) {
            EmptyStateText(stringResource(R.string.stats_congregation_no_data))
            return@StatsCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatStatsPercent(rate, locale), color = SalatPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
            state.congregationDelta?.let { delta ->
                Spacer(Modifier.width(10.dp))
                Text(
                    if (delta == 0) stringResource(R.string.stats_no_change) else stringResource(R.string.stats_vs_last, "${if (delta > 0) "+" else "−"}${formatStatsPercent(kotlin.math.abs(delta), locale)}"),
                    color = if (delta < 0) SalatError else SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
                )
            }
        }
        if (!breakdown.isLoading) {
            val entries = breakdown.breakdown.filter { it.counts.congregation + it.counts.individual > 0 }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(entries.joinToString(" · ") { entry -> "${entry.prayerType.shortLabel()} ${formatStatsPercent((entry.counts.congregation * 100) / (entry.counts.congregation + entry.counts.individual), locale)}" }, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.stats_congregation_challenge), color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        Text(stringResource(R.string.stats_congregation_day_of_40, state.congregationChallenge.currentRun), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
        Text(stringResource(R.string.stats_congregation_best_run, state.congregationChallenge.bestRun), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun QazaRecoveryCard(recovery: com.salattracker.domain.usecase.QazaRecovery) {
    val locale = LocalConfiguration.current.locales[0]
    StatsCard {
        Text(stringResource(R.string.stats_qaza_recovery), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        Spacer(Modifier.height(12.dp))
        val change = recovery.outstandingAtEnd - recovery.outstandingAtStart
        val changeText = when {
            change < 0 -> "−${formatStatsNumber(-change, locale)}"
            change > 0 -> "+${formatStatsNumber(change, locale)}"
            else -> stringResource(R.string.stats_no_change)
        }
        Text(stringResource(R.string.stats_qaza_balance, recovery.outstandingAtStart, recovery.outstandingAtEnd, changeText), color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.stats_qaza_made_up_period, recovery.madeUp), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
    }
}

// ── Trend (Vico line, 0-100, no scroll) ──────────────────────────────────────────

@Composable
private fun TrendCard(state: OverviewUiState, period: StatsPeriod) {
    StatsCard {
        Text(stringResource(R.string.stats_trend), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> ShimmerBox(Modifier.fillMaxWidth().height(180.dp))
            state.trend.count { it.completionPct != null } < 4 -> EmptyStateText(stringResource(R.string.stats_trend_four_weeks))
            else -> CompletionTrendChart(state.trend, trendBucketFor(period.days), period.days, Modifier.fillMaxWidth())
        }
    }
}

// ── By prayer (stacked status bar per prayer) ────────────────────────────────────

@Composable
private fun PrayerBreakdownCard(state: PrayerBreakdownUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_by_prayer), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) { ShimmerBox(Modifier.fillMaxWidth().height(20.dp)) }
            }
            state.breakdown.all { it.counts.countable == 0 } -> EmptyStateText(stringResource(R.string.stats_no_data_period))
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
                        color = SalatError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
                    )
                } else if (allOnTrack) {
                    Text(
                        text = stringResource(R.string.stats_all_on_track),
                        color = SalatSurfaceTint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerBarRow(entry: PrayerBreakdown, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(entry.prayerType.iconRes), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.prayerType.shortLabel(), color = SalatOnSurface, fontSize = 13.sp, fontFamily = AppUiFontFamily,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) { StatusBar(entry.counts, height = 8.dp) }
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.completionRate?.let { formatStatsPercent((it * 100).toInt(), locale) } ?: "—",
            color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = AppUiFontFamily, textAlign = TextAlign.End, modifier = Modifier.width(36.dp),
        )
    }
}

private fun formatStatsNumber(value: Int, locale: java.util.Locale): String =
    NumberFormat.getIntegerInstance(locale).format(value)

private fun formatStatsPercent(value: Int, locale: java.util.Locale): String =
    NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }.format(value / 100.0)

private fun formatStatsRatio(first: Int, second: Int, locale: java.util.Locale): String =
    "${formatStatsNumber(first, locale)}/${formatStatsNumber(second, locale)}"

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
            ),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = sampleBreakdown(listOf(0.6f, 0.9f, 0.75f, 1f, 0.95f))),
        ),
        onPeriodChanged = {},
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
            ),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = sampleBreakdown(listOf(0.4f, 0.8f, 0.7f, 0.9f, 0.85f))),
        ),
        onPeriodChanged = {},
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
        ),
        onPeriodChanged = {},
    )
}
