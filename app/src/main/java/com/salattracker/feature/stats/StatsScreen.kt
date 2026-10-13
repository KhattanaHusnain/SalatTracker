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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.GoalCompletionStat
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.StatusTrendPoint
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onStatusTrendPeriodChanged: (StatsPeriod) -> Unit,
    onGoalsBarPeriodChanged: (StatsPeriod) -> Unit,
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

            HighestStreaksCard(streaks = uiState.highestStreaks)
            MetricsSection(state = uiState.metrics)
            StatusTrendCard(state = uiState.statusTrend, onPeriodChanged = onStatusTrendPeriodChanged)
            DailyGoalsBarCard(state = uiState.goalsBar, onPeriodChanged = onGoalsBarPeriodChanged, onAddGoalClick = onAddGoalClick)

            if (uiState.showAchievement) AchievementBanner(streak = uiState.currentStreak)
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

// ── Shared period selector — horizontally-scrollable pill chips ─────────────────

@Composable
private fun PeriodToggle(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsPeriod.values().forEach { period ->
            PeriodChip(
                label      = period.displayLabel,
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

// ── Highest streaks per prayer ─────────────────────────────────────────────────

@Composable
private fun HighestStreaksCard(streaks: List<PrayerHighestStreak>) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.stats_highest_streaks), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                    Text(stringResource(R.string.stats_best_per_prayer), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
                }
                Icon(painterResource(R.drawable.ic_local_fire_department), null, tint = Color(0xFFFF9939), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                streaks.forEach { s ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatSecondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(s.prayerType.streakIconRes), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${s.highest}", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                        Text(s.prayerType.shortLabel(), color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, maxLines = 1)
                    }
                }
            }
        }
    }
}

private val PrayerType.streakIconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

// ── Metrics section (congregation / individual / missed) ─────────────────────

@Composable
private fun MetricsSection(state: MetricsUiState) {
    if (state.isLoading) MetricsGridSkeleton() else MetricsGrid(state)
}

@Composable
private fun MetricsGrid(state: MetricsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCardPrimary(stringResource(R.string.stats_congregation_pct), "${state.congregationPct}%",
            state.congregationPct / 100f, state.congregationDelta, R.drawable.ic_groups)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardSecondary(Modifier.weight(1f), stringResource(R.string.stats_individual_pct), "${state.individualPct}%",
                state.individualPct / 100f, SalatOnPrimaryContainer, R.drawable.ic_person)
            MetricCardSecondary(Modifier.weight(1f), stringResource(R.string.stats_missed_qaza), "${state.missedPct}%",
                state.missedPct / 100f, SalatTertiary, R.drawable.ic_history_edu)
        }
    }
}

@Composable
private fun MetricCardPrimary(label: String, value: String, progress: Float, delta: Int, iconRes: Int) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Box(Modifier.padding(20.dp)) {
            Icon(painterResource(iconRes), null, tint = SalatPrimary.copy(alpha = 0.05f),
                modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(8.dp, 8.dp))
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(label, color = SalatOnSurfaceVariant, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = InterFamily, letterSpacing = 1.sp)
                    Icon(painterResource(iconRes), null, tint = SalatPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(value, color = SalatPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                    if (delta != 0) Text(stringResource(R.string.stats_vs_last, "${if (delta > 0) "+" else ""}$delta"),
                        color = if (delta > 0) SalatSurfaceTint else SalatError,
                        fontSize = 11.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressBar(progress, SalatPrimaryContainer, SalatSecondaryContainer)
            }
        }
    }
}

@Composable
private fun MetricCardSecondary(modifier: Modifier, label: String, value: String, progress: Float, color: Color, iconRes: Int) {
    Surface(modifier, RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressBar(progress, color, SalatSecondaryContainer, 4.dp)
        }
    }
}

@Composable
private fun LinearProgressBar(progress: Float, color: Color, trackColor: Color, height: androidx.compose.ui.unit.Dp = 6.dp) {
    val animProg by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "p")
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(color))
    }
}

@Composable
private fun MetricsGridSkeleton() {
    val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(16.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
            Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(16.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
        }
    }
}

// ── Status trend — multi-line %-composition chart (congregation / individual / qaza) ──

// Same status colors used by the Calendar rings/legend — see SalatStatus* in core/theme/Color.kt.
private val CongregationColor = SalatStatusCongregation
private val IndividualColor   = SalatStatusIndividual
private val QazaColor         = SalatStatusQazaPending

@Composable
private fun StatusTrendCard(state: StatusTrendUiState, onPeriodChanged: (StatsPeriod) -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.stats_prayer_trend), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                    Text(stringResource(R.string.stats_composition_over, state.period.displayLabel.lowercase()), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
                }
            }
            Spacer(Modifier.height(12.dp))
            PeriodToggle(selected = state.period, onSelect = onPeriodChanged)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TrendLegendItem(CongregationColor, stringResource(R.string.status_congregation))
                TrendLegendItem(IndividualColor, stringResource(R.string.status_individual))
                TrendLegendItem(QazaColor, stringResource(R.string.status_qaza))
            }
            Spacer(Modifier.height(16.dp))
            if (state.isLoading || state.points.isEmpty()) {
                val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
            } else {
                StatusTrendCanvas(state.points)
            }
        }
    }
}

@Composable
private fun TrendLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily)
    }
}

@Composable
private fun StatusTrendCanvas(points: List<StatusTrendPoint>) {
    val animProgress by animateFloatAsState(1f, tween(1200, easing = FastOutSlowInEasing), label = "chart")
    // Percentages are always on a fixed 0-100 scale.
    val series = listOf(
        CongregationColor to points.map { it.congregationPct },
        IndividualColor   to points.map { it.individualPct },
        QazaColor         to points.map { it.qazaPct },
    )
    // Past ~12 points the line and its labels get too cramped to read, so scroll horizontally with a
    // fixed per-point slot; fewer points just fill the card width. Points are slot-centered so the
    // canvas markers line up with the labels below in both layouts.
    val scrollable = points.size > 12
    val slotWidth  = 44.dp

    val scrollState = rememberScrollState()
    // Start pinned to the right so the most recent points show without scrolling; past data is a swipe left.
    LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

    val outerModifier   = if (scrollable) Modifier.fillMaxWidth().horizontalScroll(scrollState) else Modifier.fillMaxWidth()
    val contentModifier = if (scrollable) Modifier.width(slotWidth * points.size) else Modifier.fillMaxWidth()

    Box(outerModifier) {
        Column(contentModifier) {
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val w = size.width; val h = size.height; val padH = 16.dp.toPx(); val chartH = h - padH
                val slot = if (points.isNotEmpty()) w / points.size else w
                fun cx(i: Int) = slot * (i + 0.5f)
                series.forEach { (color, values) ->
                    val ys = values.map { v -> chartH - (v / 100f) * chartH * animProgress + padH / 2 }
                    val linePath = Path().apply {
                        ys.forEachIndexed { i, y -> if (i == 0) moveTo(cx(i), y) else lineTo(cx(i), y) }
                    }
                    drawPath(linePath, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Only mark points on short ranges — dots on a dense 30-point line just smear together.
                    if (points.size <= 14) {
                        ys.forEachIndexed { i, y ->
                            drawCircle(color, 3.5.dp.toPx(), Offset(cx(i), y))
                            drawCircle(Color.White, 1.5.dp.toPx(), Offset(cx(i), y))
                        }
                    }
                }
            }
            // When scrolling, every point has its own slot so show all labels; otherwise thin them
            // out so the ones that do show stay legible instead of overlapping.
            val labelStep = if (scrollable) 1 else ((points.size - 1) / 9).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                points.forEachIndexed { i, pt ->
                    val showLabel = i % labelStep == 0 || i == points.lastIndex
                    Text(
                        text = if (showLabel) pt.label else "",
                        color = SalatOutline, fontSize = 10.sp, fontFamily = InterFamily,
                        textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── Shared bar chart primitive ─────────────────────────────────────────────────

private data class BarChartItem(val label: String, val rate: Float)

@Composable
private fun BarChartSkeleton() {
    val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Row(Modifier.fillMaxWidth().height(160.dp), Arrangement.spacedBy(12.dp), Alignment.Bottom) {
        repeat(5) { Box(Modifier.weight(1f).fillMaxHeight(0.6f).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha))) }
    }
}

@Composable
private fun BarChart(items: List<BarChartItem>) {
    val maxRate = items.maxOfOrNull { it.rate } ?: 1f
    // Past ~6 bars they'd get squeezed too thin to read, so scroll horizontally with a fixed bar
    // width instead; a handful of bars still fills the card evenly with weight(1f).
    val scrollable = items.size > 6

    val rowModifier = if (scrollable) {
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(160.dp)
    } else {
        Modifier.fillMaxWidth().height(160.dp)
    }
    Row(rowModifier, Arrangement.spacedBy(12.dp), Alignment.Bottom) {
        items.forEach { item ->
            val animRate by animateFloatAsState((item.rate / maxRate).coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar_${item.label}")
            val colModifier = if (scrollable) Modifier.width(48.dp).fillMaxHeight() else Modifier.weight(1f).fillMaxHeight()
            Column(
                modifier = colModifier,
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${(item.rate * 100).toInt()}%", color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 4.dp))
                // Bar grows within its own reserved space so it can never grow tall enough to cover the label below it.
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(animRate.coerceAtLeast(0.05f)).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(Brush.verticalGradient(listOf(SalatPrimaryContainer, SalatPrimary))))
                }
                Spacer(Modifier.height(6.dp))
                Text(item.label, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

// ── Daily goals bar chart ──────────────────────────────────────────────────────

@Composable
private fun DailyGoalsBarCard(state: GoalsBarUiState, onPeriodChanged: (StatsPeriod) -> Unit, onAddGoalClick: () -> Unit) {
    val perDay = state.period.days <= 28
    val isEmpty = if (perDay) state.dailyPoints.isEmpty() else state.items.isEmpty()

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(stringResource(R.string.stats_daily_goals), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Text(stringResource(R.string.stats_completion_rate), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
            Spacer(Modifier.height(12.dp))
            PeriodToggle(selected = state.period, onSelect = onPeriodChanged)
            Spacer(Modifier.height(20.dp))
            if (state.isLoading) {
                BarChartSkeleton()
            } else if (isEmpty) {
                Box(
                    Modifier.fillMaxWidth().clickable(onClick = onAddGoalClick).padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.stats_no_active_goals),
                        color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily, textAlign = TextAlign.Center,
                    )
                }
            } else if (perDay) {
                DailyBarChart(state.dailyPoints.map { DailyBarItem(it.date, it.completionRate) })
            } else {
                BarChart(state.items.map { BarChartItem(it.goalName.shortLabel(), it.completionRate) })
            }
        }
    }
}

private fun String.shortLabel(): String = if (length <= 8) this else take(7) + "…"

// ── Per-day bar chart (Week mode) ──────────────────────────────────────────────

private data class DailyBarItem(val date: java.time.LocalDate, val rate: Float)

private val dayNumberFormatter = DateTimeFormatter.ofPattern("d/M")
private val dayNameFormatter = DateTimeFormatter.ofPattern("EEE")

@Composable
private fun DailyBarChart(items: List<DailyBarItem>) {
    // Up to 28 daily bars (4-week range) won't fit one screen width, so scroll horizontally and
    // give each bar a fixed width instead of weight(1f) so they never get squeezed illegibly thin.
    val scrollState = rememberScrollState()
    // Start pinned to the right so the most recent days show without scrolling; past data is a swipe left.
    LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }
    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(scrollState).height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Bottom,
    ) {
        items.forEach { item ->
            val animRate by animateFloatAsState(item.rate.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "daybar_${item.date}")
            Column(
                modifier = Modifier.width(40.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${(item.rate * 100).toInt()}%", color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 4.dp))
                // Bar grows within its own reserved space so it can never grow tall enough to cover the day label below it.
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(animRate.coerceAtLeast(0.05f)).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(Brush.verticalGradient(listOf(SalatPrimaryContainer, SalatPrimary))))
                }
                Spacer(Modifier.height(6.dp))
                Text(item.date.format(dayNumberFormatter), color = SalatOnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily, textAlign = TextAlign.Center)
                Text(item.date.format(dayNameFormatter), color = SalatOnSurfaceVariant, fontSize = 9.sp, fontFamily = InterFamily, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AchievementBanner(streak: Int) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatPrimaryContainer) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(SalatPrimary), Alignment.Center) {
                Icon(painterResource(R.drawable.ic_workspace_premium), null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(stringResource(R.string.stats_achievement_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.stats_achievement_desc, streak),
                    color = SalatOnPrimaryContainer, fontSize = 14.sp, fontFamily = InterFamily, lineHeight = 20.sp)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen(
        uiState = StatsUiState(
            metrics = MetricsUiState(
                isLoading = false, period = StatsPeriod.WEEK,
                congregationPct = 65, individualPct = 25, missedPct = 10, congregationDelta = 5,
            ),
            statusTrend = StatusTrendUiState(
                isLoading = false, period = StatsPeriod.WEEK,
                points = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").mapIndexed { i, d ->
                    StatusTrendPoint(d, listOf(60,80,70,90,85,75,100)[i], listOf(40,20,20,10,15,25,0)[i], listOf(0,0,10,0,0,0,0)[i])
                },
            ),
            goalsBar = GoalsBarUiState(
                isLoading = false, period = StatsPeriod.WEEK,
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
        onStatusTrendPeriodChanged = {},
        onGoalsBarPeriodChanged    = {},
        onAddGoalClick             = {},
    )
}
