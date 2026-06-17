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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerBreakdown
import com.salattracker.domain.repository.StreakPoint
import kotlin.math.max

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
            if (uiState.isLoading) MetricsGridSkeleton() else MetricsGrid(uiState)
            StreakChartCard(points = uiState.streakPoints, period = uiState.period, isLoading = uiState.isLoading)
            PrayerBreakdownCard(breakdown = uiState.prayerBreakdown, isLoading = uiState.isLoading)
            if (!uiState.isLoading && uiState.showAchievement) AchievementBanner(streak = uiState.currentStreak)
        }
        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text("OK", color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }
    }
}

@Composable
private fun PeriodToggle(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurfaceContainerLow,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            StatsPeriod.values().forEach { period ->
                val isSelected = selected == period
                Box(
                    modifier = Modifier
                        .weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SalatPrimary else Color.Transparent)
                        .clickable { onSelect(period) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        period.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) SalatOnPrimary else SalatOnSurfaceVariant,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(uiState: StatsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCardPrimary("Congregation %", "${uiState.congregationPct}%",
            uiState.congregationPct / 100f, uiState.congregationDelta, R.drawable.ic_groups)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardSecondary(Modifier.weight(1f), "Individual %", "${uiState.individualPct}%",
                uiState.individualPct / 100f, SalatOnPrimaryContainer, R.drawable.ic_person)
            MetricCardSecondary(Modifier.weight(1f), "Missed / Qaza", "${uiState.missedPct}%",
                uiState.missedPct / 100f, SalatTertiary, R.drawable.ic_history_edu)
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
                    if (delta != 0) Text("${if (delta > 0) "+" else ""}$delta% vs last",
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

// ── Streak chart — now uses domain StreakPoint ─────────────────────────────────

@Composable
private fun StreakChartCard(points: List<StreakPoint>, period: StatsPeriod, isLoading: Boolean) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Streak History", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                    Text("Daily completion over ${period.name.lowercase()}", color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
                }
                Icon(painterResource(R.drawable.ic_workspace_premium), null, tint = Color(0xFFFF9939), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (isLoading || points.isEmpty()) {
                val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
            } else {
                LineChartCanvas(points)
            }
        }
    }
}

@Composable
private fun LineChartCanvas(points: List<StreakPoint>) {
    val animProgress by animateFloatAsState(1f, tween(1200, easing = FastOutSlowInEasing), label = "chart")
    val maxVal = max(points.maxOfOrNull { it.completedCount } ?: 5, 5).toFloat()
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val w = size.width; val h = size.height; val padH = 16.dp.toPx(); val chartH = h - padH
            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w
            val ys = points.map { pt -> chartH - (pt.completedCount / maxVal) * chartH * animProgress + padH / 2 }
            val fillPath = Path().apply {
                points.forEachIndexed { i, _ -> val x = i * stepX; if (i == 0) moveTo(x, ys[i]) else lineTo(x, ys[i]) }
                lineTo((points.size - 1) * stepX, h); lineTo(0f, h); close()
            }
            drawPath(fillPath, Brush.verticalGradient(listOf(SalatPrimary.copy(alpha = 0.15f), Color.Transparent), 0f, h))
            val linePath = Path().apply { points.forEachIndexed { i, _ -> val x = i * stepX; if (i == 0) moveTo(x, ys[i]) else lineTo(x, ys[i]) } }
            drawPath(linePath, SalatPrimary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.forEachIndexed { i, pt ->
                if (pt.completedCount > 0) {
                    drawCircle(SalatPrimary, 4.dp.toPx(), Offset(i * stepX, ys[i]))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(i * stepX, ys[i]))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.SpaceBetween) {
            points.forEach { pt -> Text(pt.label, color = SalatOutline, fontSize = 10.sp, fontFamily = InterFamily, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}

// ── Bar chart — now uses domain PrayerBreakdown ────────────────────────────────

@Composable
private fun PrayerBreakdownCard(breakdown: List<PrayerBreakdown>, isLoading: Boolean) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Prayer Breakdown", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Text("Completion rate", color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
            Spacer(Modifier.height(20.dp))
            if (isLoading || breakdown.isEmpty()) {
                val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
                Row(Modifier.fillMaxWidth().height(160.dp), Arrangement.spacedBy(12.dp), Alignment.Bottom) {
                    repeat(5) { Box(Modifier.weight(1f).fillMaxHeight(0.6f).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha))) }
                }
            } else BarChart(breakdown)
        }
    }
}

@Composable
private fun BarChart(breakdown: List<PrayerBreakdown>) {
    val maxRate = breakdown.maxOfOrNull { it.completionRate } ?: 1f
    Row(Modifier.fillMaxWidth().height(160.dp), Arrangement.spacedBy(12.dp), Alignment.Bottom) {
        breakdown.forEach { item ->
            val animRate by animateFloatAsState((item.completionRate / maxRate).coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar_${item.prayerType}")
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${(item.completionRate * 100).toInt()}%", color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, modifier = Modifier.padding(bottom = 4.dp))
                Box(Modifier.fillMaxWidth().fillMaxHeight(animRate.coerceAtLeast(0.05f)).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(Brush.verticalGradient(listOf(SalatPrimaryContainer, SalatPrimary))))
                Spacer(Modifier.height(6.dp))
                Text(item.prayerType.shortName, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily, textAlign = TextAlign.Center)
            }
        }
    }
}

private val PrayerType.shortName get() = when (this) {
    PrayerType.FAJR -> "Fajr"; PrayerType.DHUHR -> "Dhuhr"; PrayerType.ASR -> "Asr"
    PrayerType.MAGHRIB -> "Magh"; PrayerType.ISHA -> "Isha"
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
                Text("Consistent Spirit", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Spacer(Modifier.height(4.dp))
                Text("You've completed all prayers for $streak days in a row. May your steadfastness be rewarded.",
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
            isLoading = false, period = StatsPeriod.WEEK,
            congregationPct = 65, individualPct = 25, missedPct = 10, congregationDelta = 5,
            streakPoints = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").mapIndexed { i, d -> StreakPoint(d, listOf(3,5,4,5,5,4,5)[i]) },
            prayerBreakdown = PrayerType.values().mapIndexed { i, t -> PrayerBreakdown(t, listOf(1f,0.85f,0.70f,0.90f,0.60f)[i]) },
            currentStreak = 5, showAchievement = true,
        ),
        onPeriodChanged = {},
    )
}