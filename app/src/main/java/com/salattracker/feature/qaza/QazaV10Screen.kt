package com.salattracker.feature.qaza

import android.animation.ValueAnimator
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatBackground
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutline
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatSecondary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatSurface
import com.salattracker.core.theme.SalatSurfaceContainerLow
import com.salattracker.core.ui.ArchitecturalFrieze
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.Gender
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun QazaV10Screen(
    state: QazaV10State,
    onOpen: (QazaV10Page) -> Unit,
    onBack: () -> Unit,
    onDailyGoal: (Int) -> Unit,
    onOldestFirst: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onHideBadge: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onMakeUp: (PrayerType) -> Unit,
    onUndoLast: () -> Unit,
    onUndoActivity: (Long) -> Unit,
    onAddMode: (QazaAddMode) -> Unit,
    onAddDates: (LocalDate, LocalDate) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    onAddCount: (PrayerType, Int) -> Unit,
    onReason: (String?) -> Unit,
    onPreviewAdd: () -> Unit,
    onConfirmAdd: () -> Unit,
    onLogCount: (PrayerType, Int) -> Unit,
    onConfirmLog: () -> Unit,
    onOlderYears: (Int) -> Unit,
    onExcusedDays: (Int) -> Unit,
    onPreviewOlder: () -> Unit,
    onConfirmOlder: () -> Unit,
    onClearMessage: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(SalatBackground)) {
        when (state.page) {
            QazaV10Page.LANDING -> QazaLanding(
                state, onOpen, onDailyGoal, onOldestFirst, onSound, onHideBadge,
                onReducedMotion, onMakeUp, onUndoActivity,
            )
            QazaV10Page.ADD_MISSED -> AddMissedPage(
                state, onBack, onAddMode, onAddDates, onTogglePrayer, onAddCount, onReason,
                onPreviewAdd, onConfirmAdd,
            )
            QazaV10Page.LOG_SEVERAL -> LogSeveralPage(state, onBack, onOldestFirst, onLogCount, onConfirmLog)
            QazaV10Page.OLDER_MISSED -> OlderMissedPage(state, onBack, onOlderYears, onExcusedDays, onPreviewOlder, onConfirmOlder)
            QazaV10Page.DAILY_GOALS -> DailyGoalsPage(state, onBack)
        }
        state.undoCount?.let { count ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).testTag("qaza-undo"),
                color = SalatOnSurface,
                contentColor = SalatSurface,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp,
            ) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$count", Modifier.weight(1f), fontFamily = AppUiFontFamily)
                    TextButton(onClick = onUndoLast) { Text(stringResource(R.string.qaza_v10_undo), color = SalatSurface) }
                }
            }
        }
        state.message?.let { message ->
            AlertDialog(
                onDismissRequest = onClearMessage,
                confirmButton = { TextButton(onClick = onClearMessage) { Text(stringResource(android.R.string.ok)) } },
                text = {
                    Text(
                        if (message.estimated) stringResource(R.string.qaza_v10_added_estimated_result, message.added)
                        else stringResource(R.string.qaza_v10_added_result, message.added, message.skipped),
                        fontFamily = AppUiFontFamily,
                    )
                },
            )
        }
        if (state.isSaving) {
            Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = .28f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = SalatPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.qaza_v10_loading), color = Color.White, fontFamily = AppUiFontFamily)
                }
            }
        }
    }
}

@Composable
private fun QazaLanding(
    state: QazaV10State,
    onOpen: (QazaV10Page) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onOldestFirst: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onHideBadge: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onMakeUp: (PrayerType) -> Unit,
    onUndoActivity: (Long) -> Unit,
) {
    val feedback = rememberQazaFeedback(state.preferences.soundEnabled)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 96.dp)
            .testTag("qaza-page-landing"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        HeroCard(state, onDailyGoal, onSound)
        TodayCard(state) { prayer -> feedback(); onMakeUp(prayer) }
        RecentActivity(state, onUndoActivity)
        DailyStrip(state) { onOpen(QazaV10Page.DAILY_GOALS) }
        PaceCard(state)
        WeeklyFlowCard(state)
        InsightsCard(state)
        MilestonesCard(state)
        ActionCard(onOpen)
        PreferencesCard(state, onOldestFirst, onSound, onHideBadge, onReducedMotion)
    }
}

@Composable
private fun HeroCard(state: QazaV10State, onDailyGoal: (Int) -> Unit, onSound: (Boolean) -> Unit) {
    V10Card(Modifier.testTag("qaza-hero")) {
        Box(Modifier.fillMaxWidth()) {
            ArchitecturalFrieze(Modifier.align(Alignment.TopEnd).size(122.dp, 20.dp), alpha = .18f)
            IconButton(onClick = { onSound(!state.preferences.soundEnabled) }, Modifier.align(Alignment.TopStart)) {
                Icon(
                    painterResource(if (state.preferences.soundEnabled) R.drawable.ic_notifications else R.drawable.ic_bell_off),
                    null, tint = SalatPrimary,
                )
            }
        }
        ArchGoalGauge(state.outstanding, state.madeUpToday, state.todayTarget, state.preferences.reducedMotion)
        Text("${state.outstanding}", color = SalatPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
        Text(stringResource(R.string.qaza_v10_left), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
        val total = state.outstanding + state.progress.madeUpTotal
        val percent = if (total == 0) 100 else state.progress.madeUpTotal * 100 / total
        Text(stringResource(R.string.qaza_v10_made_up, state.progress.madeUpTotal, percent), color = SalatSecondary, fontSize = 13.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.qaza_v10_daily_goal), color = SalatOnSurface, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15).forEach { goal -> ChoiceChip(stringResource(R.string.qaza_v10_per_day, goal), state.preferences.dailyGoal == goal) { onDailyGoal(goal) } }
        }
    }
}

@Composable
private fun ArchGoalGauge(outstanding: Int, done: Int, target: Int, reducedMotion: Boolean) {
    val raw = if (target == 0) 1f else (done.toFloat() / target).coerceIn(0f, 1f)
    val skipAnim = reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    val progress by animateFloatAsState(raw, animationSpec = if (skipAnim) snap() else tween(650), label = "qaza-arch")
    val trackColor = SalatSecondaryContainer
    val progressColor = SalatPrimary
    val beadColor = SalatSecondary
    Canvas(Modifier.fillMaxWidth().height(92.dp).testTag("qaza-goal-arch")) {
        val stroke = 10.dp.toPx()
        val rect = Rect(stroke, stroke, size.width - stroke, size.height * 1.75f)
        drawArc(trackColor, 180f, 180f, false, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(progressColor, 180f, 180f * progress, false, topLeft = rect.topLeft, size = rect.size, style = Stroke(stroke, cap = StrokeCap.Round))
        val beadCount = 15
        repeat(beadCount) { index ->
            val x = size.width * (.15f + .70f * index / (beadCount - 1))
            drawCircle(if (index < progress * beadCount) beadColor else trackColor, 2.4.dp.toPx(), Offset(x, size.height - 5.dp.toPx()))
        }
    }
}

@Composable
private fun TodayCard(state: QazaV10State, onMakeUp: (PrayerType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_today_goal), stringResource(R.string.qaza_v10_today_progress, state.madeUpToday, state.todayTarget))
        V10Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PrayerType.values().forEach { prayer ->
                    val count = state.pendingByPrayer[prayer] ?: 0
                    val doneForPrayer = state.activities.count {
                        it.prayerType == prayer && it.newStatus == PrayerStatus.QAZA_COMPLETED &&
                            Instant.ofEpochMilli(it.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
                    }
                    val prayerTarget = minOf(state.preferences.dailyGoal / 5, count + doneForPrayer)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(prayer.iconRes), null, tint = if (count > 0) SalatPrimary else SalatOutline, modifier = Modifier.size(22.dp))
                        Text(prayer.label(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = SalatOnSurface, fontFamily = AppUiFontFamily)
                        Text("$count", color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(prayerTarget) { unit ->
                                Box(Modifier.size(5.dp).clip(CircleShape).background(if (unit < doneForPrayer) SalatSecondary else SalatSecondaryContainer))
                            }
                        }
                        FilledTonalIconButton(
                            onClick = { onMakeUp(prayer) },
                            enabled = count > 0,
                            modifier = Modifier.size(34.dp).testTag("qaza-plus-${prayer.name.lowercase()}"),
                        ) { Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp)) }
                    }
                }
            }
            Text(stringResource(R.string.qaza_v10_tap_plus), Modifier.fillMaxWidth(), color = SalatOnSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun RecentActivity(state: QazaV10State, onUndo: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_recent))
        V10Card {
            if (state.activities.isEmpty()) {
                Text(stringResource(R.string.qaza_v10_nothing_recent), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
            } else state.activities.take(5).forEachIndexed { index, activity ->
                if (index > 0) HorizontalDivider(color = SalatOutline.copy(alpha = .2f))
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(activity.prayerType.iconRes), null, tint = SalatSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(activity.prayerType.label(), color = SalatOnSurface, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
                        Text(activity.prayerDate.toString(), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                    }
                    TextButton(onClick = { onUndo(activity.id) }) { Text(stringResource(R.string.qaza_v10_undo)) }
                }
            }
        }
    }
}

@Composable
private fun DailyStrip(state: QazaV10State, onOpen: () -> Unit) {
    val byDay = state.activities.filter { it.newStatus == PrayerStatus.QAZA_COMPLETED }.groupingBy {
        Instant.ofEpochMilli(it.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }.eachCount()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_daily_goals))
        V10Card(Modifier.clickable(onClick = onOpen)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (6 downTo 0).forEach { offset ->
                    val date = LocalDate.now().minusDays(offset.toLong())
                    MiniArch(date.dayOfMonth, (byDay[date] ?: 0).toFloat() / state.preferences.dailyGoal)
                }
            }
        }
    }
}

@Composable
private fun MiniArch(day: Int, progress: Float) {
    val trackColor = SalatSecondaryContainer
    val progressColor = SalatPrimary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(trackColor, 180f, 180f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                drawArc(progressColor, 180f, 180f * progress.coerceIn(0f, 1f), false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Text("$day", fontSize = 11.sp, color = SalatOnSurface, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun PaceCard(state: QazaV10State) {
    var whatIf by remember { mutableFloatStateOf((state.preferences.dailyGoal / 5).coerceIn(1, 5).toFloat()) }
    val perDay = whatIf.roundToInt() * 5
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_pace))
        V10Card {
            Text(stringResource(R.string.qaza_v10_left), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            PaceChart(state.outstanding, perDay, state.preferences.reducedMotion)
            Text(state.progress.eta?.let { "${it.value} ${it.unit.name.lowercase()}" } ?: stringResource(R.string.qaza_pace_start), color = SalatPrimary, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            Text(stringResource(R.string.qaza_v10_what_if), color = SalatOnSurface, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
            Slider(value = whatIf, onValueChange = { whatIf = it }, valueRange = 1f..5f, steps = 3)
            Text(stringResource(R.string.qaza_v10_finish_days, perDay, (state.outstanding + perDay - 1) / perDay), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun PaceChart(outstanding: Int, perDay: Int, reducedMotion: Boolean) {
    val gridColor = SalatOutline.copy(alpha = .25f)
    val lineColor = SalatPrimary
    val skipAnim = reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    val targetDrop = if (outstanding <= 0) 0f else (perDay / 25f).coerceIn(0.28f, 1f)
    val dropFraction by animateFloatAsState(
        targetValue = targetDrop,
        animationSpec = if (skipAnim) snap() else tween(350),
        label = "qaza-pace-drop",
    )
    Canvas(Modifier.fillMaxWidth().height(120.dp).testTag("qaza-pace-chart")) {
        drawLine(gridColor, Offset(0f, 12f), Offset(size.width, 12f))
        drawLine(gridColor, Offset(0f, size.height - 12f), Offset(size.width, size.height - 12f))
        val path = Path()
        val startY = if (outstanding <= 0) size.height - 18f else 18f
        val endY = if (outstanding <= 0) size.height - 18f else 18f + (size.height - 36f) * dropFraction
        repeat(8) { index ->
            val t = index / 7f
            val x = size.width * t
            val y = startY + (endY - startY) * t
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(lineColor, 4.dp.toPx(), Offset(size.width, endY))
    }
}

@Composable
private fun WeeklyFlowCard(state: QazaV10State) {
    val since = LocalDate.now().minusDays(6)
    val week = state.activities.filter {
        Instant.ofEpochMilli(it.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate() >= since
    }
    val added = week.count { it.newStatus == PrayerStatus.QAZA_PENDING }
    val made = week.count { it.newStatus == PrayerStatus.QAZA_COMPLETED }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_weekly))
        V10Card {
            MetricBar(stringResource(R.string.qaza_v10_new), added, maxOf(added, made, 1), SalatError)
            MetricBar(stringResource(R.string.qaza_v10_out), made, maxOf(added, made, 1), SalatPrimary)
        }
    }
}

@Composable
private fun InsightsCard(state: QazaV10State) {
    val missed = state.activities.filter { it.newStatus == PrayerStatus.QAZA_PENDING }.take(200)
    val top = missed.groupingBy { it.prayerType }.eachCount().maxByOrNull { it.value }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_insights))
        V10Card {
            if (top == null) Text(stringResource(R.string.qaza_v10_history_empty), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
            else {
                Text(top.key.label(), color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
                MetricBar(top.key.label(), top.value, missed.size.coerceAtLeast(1), SalatSecondary)
                val reason = missed.mapNotNull { it.reasonTag }.groupingBy { it }.eachCount().maxByOrNull { it.value }
                reason?.let { Text(localizedQazaReason(it.key), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
                val day = missed.groupingBy { it.prayerDate.dayOfWeek }.eachCount().maxByOrNull { it.value }
                day?.let { Text(it.key.name.lowercase().replaceFirstChar(Char::titlecase), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
            }
        }
    }
}

@Composable
private fun MilestonesCard(state: QazaV10State) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.qaza_v10_milestones))
        V10Card {
            listOf(1, 10, 50, 100).forEach { target ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_workspace_premium), null, tint = SalatSecondary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(if (target == 1) stringResource(R.string.qaza_v10_first_made_up) else "$target", Modifier.weight(1f), fontFamily = AppUiFontFamily)
                    Icon(painterResource(if (state.progress.madeUpTotal >= target) R.drawable.ic_check_circle_filled else R.drawable.ic_radio_unchecked), null, tint = SalatPrimary)
                }
            }
        }
    }
}

@Composable
private fun ActionCard(onOpen: (QazaV10Page) -> Unit) {
    V10Card {
        ActionRow(R.drawable.ic_calendar_today, stringResource(R.string.qaza_v10_add)) { onOpen(QazaV10Page.ADD_MISSED) }
        ActionRow(R.drawable.ic_pending_actions, stringResource(R.string.qaza_v10_log)) { onOpen(QazaV10Page.LOG_SEVERAL) }
        ActionRow(R.drawable.ic_history_edu, stringResource(R.string.qaza_v10_older)) { onOpen(QazaV10Page.OLDER_MISSED) }
        ActionRow(R.drawable.ic_nav_goals, stringResource(R.string.qaza_v10_daily_goals)) { onOpen(QazaV10Page.DAILY_GOALS) }
    }
}

@Composable
private fun PreferencesCard(state: QazaV10State, onOldestFirst: (Boolean) -> Unit, onSound: (Boolean) -> Unit, onHideBadge: (Boolean) -> Unit, onReducedMotion: (Boolean) -> Unit) {
    V10Card {
        SwitchRow(stringResource(R.string.qaza_v10_privacy), state.preferences.hideNavigationBadge, onHideBadge)
        SwitchRow(stringResource(R.string.qaza_v10_sound), state.preferences.soundEnabled, onSound)
        SwitchRow(stringResource(R.string.qaza_v10_reduce_motion), state.preferences.reducedMotion, onReducedMotion)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.qaza_v10_order), Modifier.weight(1f), color = SalatOnSurface, fontFamily = AppUiFontFamily)
            TextButton(onClick = { onOldestFirst(!state.preferences.oldestFirst) }) {
                Text(stringResource(if (state.preferences.oldestFirst) R.string.qaza_v10_oldest_first else R.string.qaza_v10_newest_first))
            }
        }
    }
}

@Composable
private fun AddMissedPage(
    state: QazaV10State,
    onBack: () -> Unit,
    onMode: (QazaAddMode) -> Unit,
    onDates: (LocalDate, LocalDate) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    onCount: (PrayerType, Int) -> Unit,
    onReason: (String?) -> Unit,
    onPreview: () -> Unit,
    onConfirm: () -> Unit,
) {
    FormPage(stringResource(R.string.qaza_v10_add), onBack, "qaza-page-add") {
        SegmentedChoice(
            listOf(stringResource(R.string.qaza_v10_by_dates), stringResource(R.string.qaza_v10_by_count)),
            if (state.addForm.mode == QazaAddMode.DATES) 0 else 1,
        ) { onMode(if (it == 0) QazaAddMode.DATES else QazaAddMode.COUNT) }
        if (state.addForm.mode == QazaAddMode.DATES) {
            DateAdjustRow(stringResource(R.string.qaza_v10_from), state.addForm.from) { onDates(it, state.addForm.to) }
            DateAdjustRow(stringResource(R.string.qaza_v10_to), state.addForm.to) { onDates(state.addForm.from, it) }
            Text(stringResource(R.string.qaza_v10_which), style = MaterialTheme.typography.titleMedium, color = SalatOnSurface, fontFamily = AppTitleFontFamily)
            PrayerSelector(state.addForm.prayers, onTogglePrayer)
        } else PrayerType.values().forEach { prayer -> CounterRow(prayer, state.addForm.counts[prayer] ?: 0, 100_000) { onCount(prayer, it) } }
        Text(stringResource(R.string.qaza_v10_reason), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Overslept" to R.string.qaza_v10_reason_overslept,
                "Work" to R.string.qaza_v10_reason_work,
                "Travel" to R.string.qaza_v10_reason_travel,
                "Forgot" to R.string.qaza_v10_reason_forgot,
                "Illness" to R.string.qaza_v10_reason_illness,
            ).forEach { (reason, label) ->
                ChoiceChip(stringResource(label), state.addForm.reason == reason) { onReason(reason.takeUnless { it == state.addForm.reason }) }
            }
        }
        PreviewAndConfirm(state.addForm.preview, onPreview, onConfirm)
    }
}

@Composable
private fun LogSeveralPage(state: QazaV10State, onBack: () -> Unit, onOrder: (Boolean) -> Unit, onCount: (PrayerType, Int) -> Unit, onConfirm: () -> Unit) {
    FormPage(stringResource(R.string.qaza_v10_log), onBack, "qaza-page-log") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.qaza_v10_order), Modifier.weight(1f), fontFamily = AppUiFontFamily)
            ChoiceChip(stringResource(if (state.preferences.oldestFirst) R.string.qaza_v10_oldest_first else R.string.qaza_v10_newest_first), true) { onOrder(!state.preferences.oldestFirst) }
        }
        PrayerType.values().forEach { prayer -> CounterRow(prayer, state.logCounts[prayer] ?: 0, state.pendingByPrayer[prayer] ?: 0) { onCount(prayer, it) } }
        val total = state.logCounts.values.sum()
        Button(onClick = onConfirm, enabled = total > 0, modifier = Modifier.fillMaxWidth().testTag("qaza-log-confirm")) { Text(stringResource(R.string.qaza_v10_log_count, total)) }
    }
}

@Composable
private fun OlderMissedPage(state: QazaV10State, onBack: () -> Unit, onYears: (Int) -> Unit, onExcused: (Int) -> Unit, onPreview: () -> Unit, onConfirm: () -> Unit) {
    FormPage(stringResource(R.string.qaza_v10_older), onBack, "qaza-page-older") {
        Text(stringResource(R.string.qaza_v10_how_long), color = SalatOnSurface, fontFamily = AppUiFontFamily)
        Text(stringResource(R.string.qaza_v10_years, state.olderForm.years), color = SalatPrimary, fontSize = 24.sp, fontFamily = AppTitleFontFamily)
        Slider(value = state.olderForm.years.toFloat(), onValueChange = { onYears(it.roundToInt()) }, valueRange = 1f..30f, steps = 28)
        if (state.gender == Gender.FEMALE) {
            Text(stringResource(R.string.qaza_v10_excused), color = SalatOnSurface, fontFamily = AppUiFontFamily)
            CounterValue(state.olderForm.excusedDaysPerMonth, 0, 15, onExcused)
        }
        Text(stringResource(R.string.qaza_v10_estimate_note), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        PreviewAndConfirm(state.olderForm.preview, onPreview, onConfirm)
    }
}

@Composable
private fun DailyGoalsPage(state: QazaV10State, onBack: () -> Unit) {
    val byDay = state.activities.filter { it.newStatus == PrayerStatus.QAZA_COMPLETED }.groupingBy {
        Instant.ofEpochMilli(it.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }.eachCount()
    FormPage(stringResource(R.string.qaza_v10_daily_goals), onBack, "qaza-page-goals") {
        val days = (29 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val hit = days.count { (byDay[it] ?: 0) >= state.preferences.dailyGoal }
        Text(stringResource(R.string.qaza_v10_goal_hit, hit, days.size), color = SalatPrimary, fontSize = 18.sp, fontFamily = AppTitleFontFamily)
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { date -> MiniArch(date.dayOfMonth, (byDay[date] ?: 0).toFloat() / state.preferences.dailyGoal) }
            }
        }
        if (state.activities.isEmpty()) Text(stringResource(R.string.qaza_v10_history_empty), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun FormPage(title: String, onBack: () -> Unit, tag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 96.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.qaza_v10_back), tint = SalatPrimary) }
            Text(title, color = SalatPrimary, fontSize = 24.sp, fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun PreviewAndConfirm(preview: QazaPreviewSummary?, onPreview: () -> Unit, onConfirm: () -> Unit) {
    if (preview == null) OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth().testTag("qaza-preview")) { Text(stringResource(R.string.qaza_v10_preview)) }
    else {
        V10Card { Text(stringResource(R.string.qaza_v10_preview_summary, preview.added, preview.skipped), color = SalatOnSurface, fontFamily = AppUiFontFamily) }
        Button(onClick = onConfirm, enabled = preview.added > 0, modifier = Modifier.fillMaxWidth().testTag("qaza-confirm")) { Text(stringResource(R.string.qaza_v10_add_count, preview.added)) }
    }
}

@Composable
private fun DateAdjustRow(label: String, value: LocalDate, onChange: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(54.dp), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily)
        IconButton(onClick = { onChange(value.minusDays(1)) }) { Icon(painterResource(R.drawable.ic_chevron_left), null) }
        Text(value.format(DateTimeFormatter.ISO_LOCAL_DATE), Modifier.weight(1f), textAlign = TextAlign.Center, color = SalatOnSurface, fontFamily = AppUiFontFamily)
        IconButton(onClick = { onChange(value.plusDays(1)) }) { Icon(painterResource(R.drawable.ic_chevron_right), null) }
    }
}

@Composable
private fun PrayerSelector(selected: Set<PrayerType>, onToggle: (PrayerType) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        PrayerType.values().forEach { prayer ->
            val isSelected = prayer in selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) SalatPrimary.copy(alpha = 0.14f) else SalatSurface)
                    .border(1.dp, if (isSelected) SalatPrimary else SalatSecondaryContainer, RoundedCornerShape(12.dp)).clickable { onToggle(prayer) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(prayer.iconRes), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
                Text(prayer.label(), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isSelected) SalatPrimary else SalatOnSurface, fontFamily = AppUiFontFamily)
            }
        }
    }
}

@Composable
private fun CounterRow(prayer: PrayerType, value: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(prayer.iconRes), null, tint = SalatSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp)); Text(prayer.label(), Modifier.weight(1f), color = SalatOnSurface, fontFamily = AppUiFontFamily)
        CounterValue(value, 0, max, onChange)
    }
}

@Composable
private fun CounterValue(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min, modifier = Modifier.size(34.dp)) { Icon(painterResource(R.drawable.ic_remove), null, Modifier.size(16.dp)) }
        Text("$value", Modifier.width(52.dp), textAlign = TextAlign.Center, color = SalatOnSurface, fontFamily = AppTitleFontFamily)
        FilledTonalIconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max, modifier = Modifier.size(34.dp)) { Icon(painterResource(R.drawable.ic_add), null, Modifier.size(16.dp)) }
    }
}

@Composable
private fun SegmentedChoice(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label -> ChoiceChip(label, selected == index, Modifier.weight(1f)) { onSelect(index) } }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) SalatPrimary.copy(alpha = 0.14f) else SalatSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) SalatPrimary else SalatSecondaryContainer),
    ) { Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), textAlign = TextAlign.Center, color = if (selected) SalatPrimary else SalatOnSurface, fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 1) }
}

@Composable
private fun V10Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SalatSurface, border = androidx.compose.foundation.BorderStroke(1.dp, SalatSecondaryContainer)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = SalatOnSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
        trailing?.let { Text(it, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily) }
    }
}

@Composable
private fun ActionRow(icon: Int, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, tint = SalatPrimary, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), color = SalatOnSurface, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
        Icon(painterResource(R.drawable.ic_arrow_forward), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = SalatOnSurface, fontFamily = AppUiFontFamily)
        Switch(checked, onChange)
    }
}

@Composable
private fun MetricBar(label: String, value: Int, max: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(72.dp), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(SalatSecondaryContainer)) {
            Box(Modifier.fillMaxWidth((value.toFloat() / max).coerceIn(0f, 1f)).height(8.dp).background(color))
        }
        Text("$value", Modifier.width(34.dp), textAlign = TextAlign.End, color = SalatOnSurface, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun rememberQazaFeedback(soundEnabled: Boolean): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30) }
    DisposableEffect(tone) { onDispose { tone.release() } }
    return remember(soundEnabled, haptic, view, tone) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            if (soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_ACK, 70)
        }
    }
}

@Composable
private fun localizedQazaReason(reason: String): String = when (reason) {
    "Overslept" -> stringResource(R.string.qaza_v10_reason_overslept)
    "Work" -> stringResource(R.string.qaza_v10_reason_work)
    "Travel" -> stringResource(R.string.qaza_v10_reason_travel)
    "Forgot" -> stringResource(R.string.qaza_v10_reason_forgot)
    "Illness" -> stringResource(R.string.qaza_v10_reason_illness)
    "OLDER_ESTIMATE" -> stringResource(R.string.qaza_v10_reason_older_estimate)
    else -> reason
}
