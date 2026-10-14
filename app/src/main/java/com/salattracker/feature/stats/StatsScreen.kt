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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StatusTrendPoint
import com.salattracker.domain.usecase.weakestPrayer

// ── Stats Screen ─────────────────────────────────────────────────────────────
// A today-vs-history dashboard: one shared period selector drives everything except Streak
// Records (which is all-time by definition). Order: summary -> status mix over time -> by
// prayer -> daily goals -> streak records, reading as "where do I stand" before "what's my
// long-run consistency".

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
            SummaryCard(state = uiState.summary)
            StatusMixCard(state = uiState.statusMix)
            PrayerBreakdownCard(state = uiState.prayerBreakdown)
            DailyGoalsCard(state = uiState.goals, period = uiState.period, onAddGoalClick = onAddGoalClick)
            StreakRecordsCard(
                currentStreak   = uiState.currentStreak,
                showAchievement = uiState.showAchievement,
                streaks         = uiState.highestStreaks,
            )
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

// ── Shared period selector — one row drives every section ───────────────────────

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
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
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

// ── Summary (prayed on time %, composition bar) ─────────────────────────────────

// Same status colors used by the Calendar rings/legend — see SalatStatus* in core/theme/Color.kt.
private val CongregationColor = SalatStatusCongregation
private val IndividualColor   = SalatStatusIndividual
private val QazaColor         = SalatStatusQazaPending

@Composable
private fun SummaryCard(state: SummaryUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_prayed_on_time), color = SalatOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = InterFamily, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        if (state.isLoading) {
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(36.dp))
            Spacer(Modifier.height(16.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(10.dp))
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${state.completionPct}%", color = SalatPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                if (state.completionDelta != 0) {
                    Text(
                        text = stringResource(R.string.stats_vs_last, "${if (state.completionDelta > 0) "+" else ""}${state.completionDelta}"),
                        color = if (state.completionDelta > 0) SalatSurfaceTint else SalatError,
                        fontSize = 11.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            CompositionBar(state.congregationPct, state.individualPct, state.missedPct)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TrendLegendItem(CongregationColor, stringResource(R.string.status_congregation))
                TrendLegendItem(IndividualColor, stringResource(R.string.status_individual))
                TrendLegendItem(QazaColor, stringResource(R.string.status_qaza))
            }
        }
    }
}

/** A single 100%-stacked bar: congregation, then individual, then qaza, with a grey remainder
 * for everything else (unrecorded prayers, or simply no data in this period). */
@Composable
private fun CompositionBar(congregationPct: Int, individualPct: Int, missedPct: Int) {
    val remainder = (100 - congregationPct - individualPct - missedPct).coerceAtLeast(0)
    val animCongregation by animateFloatAsState(congregationPct.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "c")
    val animIndividual by animateFloatAsState(individualPct.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "i")
    val animMissed by animateFloatAsState(missedPct.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "m")
    val animRemainder by animateFloatAsState(remainder.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "r")

    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
        if (animCongregation > 0f) Box(Modifier.weight(animCongregation).fillMaxHeight().background(CongregationColor))
        if (animIndividual > 0f) Box(Modifier.weight(animIndividual).fillMaxHeight().background(IndividualColor))
        if (animMissed > 0f) Box(Modifier.weight(animMissed).fillMaxHeight().background(QazaColor))
        if (animRemainder > 0f) Box(Modifier.weight(animRemainder).fillMaxHeight().background(SalatSurfaceVariant))
    }
}

@Composable
private fun TrendLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily)
    }
}

// ── Status mix over time (Vico stacked columns) ─────────────────────────────────

@Composable
private fun StatusMixCard(state: StatusMixUiState) {
    StatsCard {
        Text(stringResource(R.string.stats_status_mix), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TrendLegendItem(CongregationColor, stringResource(R.string.status_congregation))
            TrendLegendItem(IndividualColor, stringResource(R.string.status_individual))
            TrendLegendItem(QazaColor, stringResource(R.string.status_qaza))
        }
        Spacer(Modifier.height(16.dp))
        val hasData = state.points.any { it.congregationPct > 0 || it.individualPct > 0 || it.qazaPct > 0 }
        when {
            state.isLoading -> ShimmerBox(Modifier.fillMaxWidth().height(200.dp))
            !hasData         -> EmptyStateText(stringResource(R.string.stats_no_data_period), Modifier.height(200.dp))
            else              -> StatusMixChart(state.points, Modifier.fillMaxWidth())
        }
    }
}

// ── By prayer (completion rate per prayer, horizontal bars) ────────────────────

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
                    state.breakdown.forEach { entry ->
                        LabeledBarRow(label = entry.prayerType.shortLabel(), rate = entry.completionRate, iconRes = entry.prayerType.iconRes)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val weakest = remember(state.breakdown) { weakestPrayer(state.breakdown) }
                if (weakest != null) {
                    Text(
                        text = stringResource(R.string.stats_weakest_prayer, weakest.prayerType.shortLabel()),
                        color = SalatError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.stats_all_on_track),
                        color = SalatSurfaceTint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
                    )
                }
            }
        }
    }
}

/** One labelled row: an optional icon, a short label, a progress bar, and a trailing percentage.
 * Shared by the per-prayer breakdown and the per-goal breakdown (90D/1Y). */
@Composable
private fun LabeledBarRow(label: String, rate: Float, modifier: Modifier = Modifier, iconRes: Int? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label, color = SalatOnSurface, fontSize = 13.sp, fontFamily = InterFamily,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(76.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) { LinearProgressBar(rate, SalatPrimaryContainer, SalatSecondaryContainer, 8.dp) }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${(rate * 100).toInt()}%", color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily, textAlign = TextAlign.End, modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun LinearProgressBar(progress: Float, color: Color, trackColor: Color, height: androidx.compose.ui.unit.Dp = 6.dp) {
    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "p")
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(color))
    }
}

// ── Daily goals (Vico columns for <=40 days, per-goal bars beyond) ─────────────

@Composable
private fun DailyGoalsCard(state: GoalsUiState, period: StatsPeriod, onAddGoalClick: () -> Unit) {
    val perDay = period.days <= 40
    val isEmpty = if (perDay) state.dailyPoints.isEmpty() else state.items.isEmpty()

    StatsCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.stats_daily_goals), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
            Text(stringResource(R.string.stats_completion_rate), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> ShimmerBox(Modifier.fillMaxWidth().height(200.dp))
            isEmpty -> EmptyStateText(stringResource(R.string.stats_no_active_goals), onClick = onAddGoalClick)
            perDay -> DailyGoalsChart(state.dailyPoints, Modifier.fillMaxWidth())
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.items.forEach { item -> LabeledBarRow(label = item.goalName.shortLabel(), rate = item.completionRate) }
            }
        }
    }
}

private fun String.shortLabel(): String = if (length <= 10) this else take(9) + "…"

// ── Streak records (current + best per prayer + achievement, all-time) ─────────

@Composable
private fun StreakRecordsCard(currentStreak: Int, showAchievement: Boolean, streaks: List<PrayerHighestStreak>) {
    StatsCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.stats_streak_records), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Text(stringResource(R.string.stats_all_time), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_local_fire_department), null, tint = Color(0xFFFF9939), modifier = Modifier.size(22.dp))
                Text("$currentStreak", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.stats_best_per_prayer), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            streaks.forEach { s ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(s.prayerType.iconRes), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${s.highest}", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                    Text(s.prayerType.shortLabel(), color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, maxLines = 1)
                }
            }
        }
        if (showAchievement) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SalatOutlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(SalatPrimaryContainer), Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_workspace_premium), null, tint = SalatOnPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(stringResource(R.string.stats_achievement_title), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.stats_achievement_desc, currentStreak),
                        color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen(
        uiState = StatsUiState(
            period = StatsPeriod.WEEK,
            summary = SummaryUiState(
                isLoading = false,
                congregationPct = 65, individualPct = 25, missedPct = 10, completionPct = 90, completionDelta = 5,
            ),
            statusMix = StatusMixUiState(
                isLoading = false,
                points = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").mapIndexed { i, d ->
                    StatusTrendPoint(d, listOf(60,80,70,90,85,75,100)[i], listOf(40,20,20,10,15,25,0)[i], listOf(0,0,10,0,0,0,0)[i])
                },
            ),
            prayerBreakdown = PrayerBreakdownUiState(
                isLoading = false,
                breakdown = PrayerType.values().mapIndexed { i, t -> PrayerBreakdown(t, listOf(0.6f,0.9f,0.75f,1f,0.95f)[i]) },
            ),
            goals = GoalsUiState(
                isLoading = false,
                dailyPoints = (6 downTo 0).map { daysAgo ->
                    GoalDailyPoint(java.time.LocalDate.now().minusDays(daysAgo.toLong()), listOf(0.4f,0.6f,1f,0.8f,0.5f,0.9f,0.74f)[6 - daysAgo])
                },
                items = listOf(
                    GoalCompletionStat("Tahajjud Prayer", com.salattracker.domain.model.GoalCategory.SALAT, 0.8f),
                    GoalCompletionStat("Read Quran", com.salattracker.domain.model.GoalCategory.QURAN, 0.55f),
                    GoalCompletionStat("Morning Adhkar", com.salattracker.domain.model.GoalCategory.DHIKR, 1f),
                ),
            ),
            currentStreak = 5, showAchievement = true,
            highestStreaks = PrayerType.values().mapIndexed { i, t -> PrayerHighestStreak(t, listOf(42,20,15,8,30)[i]) },
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
            summary = SummaryUiState(
                isLoading = false,
                congregationPct = 40, individualPct = 30, missedPct = 5, completionPct = 70, completionDelta = -3,
            ),
            statusMix = StatusMixUiState(
                isLoading = false,
                points = (0..11).map { StatusTrendPoint("W${it + 1}", (50..95).random(), (5..30).random(), (0..10).random()) },
            ),
            prayerBreakdown = PrayerBreakdownUiState(
                isLoading = false,
                breakdown = PrayerType.values().mapIndexed { i, t -> PrayerBreakdown(t, listOf(0.4f,0.8f,0.7f,0.9f,0.85f)[i]) },
            ),
            goals = GoalsUiState(
                isLoading = false,
                items = listOf(
                    GoalCompletionStat("Tahajjud Prayer", com.salattracker.domain.model.GoalCategory.SALAT, 0.65f),
                    GoalCompletionStat("Read Quran", com.salattracker.domain.model.GoalCategory.QURAN, 0.4f),
                ),
            ),
            currentStreak = 0, showAchievement = false,
            highestStreaks = PrayerType.values().mapIndexed { i, t -> PrayerHighestStreak(t, listOf(42,20,15,8,30)[i]) },
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
            period = StatsPeriod.WEEK,
            summary = SummaryUiState(isLoading = false, congregationPct = 0, individualPct = 0, missedPct = 0, completionPct = 0, completionDelta = 0),
            statusMix = StatusMixUiState(isLoading = false, points = emptyList()),
            prayerBreakdown = PrayerBreakdownUiState(isLoading = false, breakdown = emptyList()),
            goals = GoalsUiState(isLoading = false),
            currentStreak = 0, showAchievement = false,
            highestStreaks = PrayerType.values().map { PrayerHighestStreak(it, 0) },
        ),
        onPeriodChanged = {},
        onAddGoalClick  = {},
    )
}
