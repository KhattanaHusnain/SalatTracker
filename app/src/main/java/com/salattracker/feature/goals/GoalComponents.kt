package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.usecase.GoalDayState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Shared building blocks for the Goals list (7-day strip) and the goal details calendar ──

/** One day of a goal as drawn in the strip or calendar. [amount] is that day's logged total. */
data class GoalDayUi(
    val date: LocalDate,
    val state: GoalDayState,
    val amount: Int,
    val isToday: Boolean = false,
    /** False for the leading/trailing days of neighbouring months in the calendar grid. */
    val inMonth: Boolean = true,
)

/** State of the "log a day" bottom sheet. */
data class LogSheetUi(
    val goalId: Long,
    val goalName: String,
    val date: LocalDate,
    val amount: Int,
    val skipped: Boolean,
    val target: Int,
    val unit: String,
    /** Excusing a day only makes sense for daily goals. */
    val canSkip: Boolean,
)

@Composable
internal fun rememberDayLabelFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }
}

private fun GoalDayState.labelRes(): Int = when (this) {
    GoalDayState.DONE    -> R.string.goal_day_done
    GoalDayState.PARTIAL -> R.string.goal_day_partial
    GoalDayState.MISSED  -> R.string.goal_day_missed
    GoalDayState.SKIPPED -> R.string.goal_day_excused
    GoalDayState.OPEN    -> R.string.goal_day_open
    GoalDayState.NOT_DUE -> R.string.goal_day_not_due
    GoalDayState.LOGGED  -> R.string.goal_day_logged
    GoalDayState.NONE    -> R.string.goal_day_none
    GoalDayState.LOCKED  -> R.string.goal_day_locked
}

// ── Streak badge ─────────────────────────────────────────────────────────────

@Composable
fun StreakBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SalatTertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(R.drawable.ic_local_fire_department), null, tint = SalatOnTertiaryContainer, modifier = Modifier.size(14.dp))
        Text("$count", color = SalatOnTertiaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
    }
}

// ── Day cell ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalDayCell(
    day: GoalDayUi,
    target: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val formatter = rememberDayLabelFormatter()
    val description = stringResource(R.string.goal_day_cd, day.date.format(formatter), stringResource(day.state.labelRes()))
    val locked = day.state == GoalDayState.LOCKED

    val container = when (day.state) {
        GoalDayState.SKIPPED -> SalatStatusExcused.copy(alpha = 0.25f)
        GoalDayState.LOGGED  -> SalatSecondaryContainer
        GoalDayState.NONE    -> SalatSurfaceContainer
        GoalDayState.LOCKED  -> SalatSurfaceContainerLow
        else                 -> Color.Transparent
    }
    val borderStroke = when (day.state) {
        GoalDayState.MISSED -> BorderStroke(1.5.dp, SalatError.copy(alpha = 0.6f))
        GoalDayState.OPEN   -> BorderStroke(1.5.dp, SalatPrimary)
        else                -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (day.inMonth) 1f else 0.4f)
            .clip(CircleShape)
            .background(container)
            .let { if (borderStroke != null) it.border(borderStroke, CircleShape) else it }
            .let { if (day.isToday && !locked) it.border(1.dp, SalatPrimary.copy(alpha = 0.35f), CircleShape) else it }
            .combinedClickable(
                enabled = !locked,
                onClickLabel = stringResource(R.string.goal_day_click_label),
                onLongClickLabel = stringResource(R.string.goal_day_long_click_label),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (day.state) {
            GoalDayState.DONE ->
                Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatPrimaryContainer, modifier = Modifier.fillMaxSize())
            GoalDayState.PARTIAL -> {
                val fraction = if (target > 0) (day.amount.toFloat() / target).coerceIn(0f, 1f) else 0f
                Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    val arcSize = Size(this.size.width, this.size.height)
                    drawArc(SalatSecondaryContainer, 0f, 360f, false, Offset.Zero, arcSize, style = stroke)
                    drawArc(SalatPrimaryContainer, -90f, 360f * fraction, false, Offset.Zero, arcSize, style = stroke)
                }
                Text("${day.amount}", color = SalatPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
            }
            GoalDayState.MISSED ->
                Icon(painterResource(R.drawable.ic_close), null, tint = SalatError.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            GoalDayState.SKIPPED ->
                Icon(painterResource(R.drawable.ic_bedtime), null, tint = SalatStatusExcused, modifier = Modifier.size(16.dp))
            GoalDayState.OPEN ->
                Icon(painterResource(R.drawable.ic_add), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
            GoalDayState.NOT_DUE ->
                Box(Modifier.size(6.dp).clip(CircleShape).background(SalatOutlineVariant))
            GoalDayState.LOGGED ->
                Text("${day.amount}", color = SalatOnSecondaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
            GoalDayState.NONE, GoalDayState.LOCKED -> Unit
        }
    }
}

// ── 7-day strip ──────────────────────────────────────────────────────────────

@Composable
fun SevenDayStrip(
    days: List<GoalDayUi>,
    target: Int,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = day.date.dayOfWeek.shortLabel(),
                    color = if (day.isToday) SalatPrimary else SalatOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = InterFamily,
                    maxLines = 1,
                )
                GoalDayCell(
                    day = day,
                    target = target,
                    onClick = { onDayClick(day.date) },
                    onLongClick = { onDayLongClick(day.date) },
                )
            }
        }
    }
}

// ── Log-day bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDaySheet(
    state: LogSheetUi,
    onAmountChanged: (Int) -> Unit,
    onSkippedChanged: (Boolean) -> Unit,
    onMarkDone: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val formatter = rememberDayLabelFormatter()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurfaceContainerLowest) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.goalName, color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily, textAlign = TextAlign.Center)
            Text(state.date.format(formatter), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily)
            Spacer(Modifier.height(20.dp))

            val amountEnabled = !state.skipped
            Text(stringResource(R.string.goal_log_amount), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                FilledTonalIconButton(
                    onClick = { onAmountChanged((state.amount - 1).coerceAtLeast(0)) },
                    enabled = amountEnabled && state.amount > 0,
                ) { Text("−", fontSize = 22.sp) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.amount}",
                        color = if (amountEnabled) SalatPrimary else SalatOutline,
                        fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily,
                    )
                    Text("/ ${state.target} ${goalUnitLabel(state.unit)}", color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                }
                FilledTonalIconButton(
                    onClick = { onAmountChanged((state.amount + 1).coerceAtMost(MAX_LOG_AMOUNT)) },
                    enabled = amountEnabled,
                ) { Text("+", fontSize = 22.sp) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onMarkDone, enabled = amountEnabled, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.action_mark_done), color = SalatPrimary, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
            }

            if (state.canSkip) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.goal_log_excused), color = SalatOnSurface, fontSize = 14.sp, fontFamily = InterFamily)
                    Switch(
                        checked = state.skipped,
                        onCheckedChange = onSkippedChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SalatOnPrimary, checkedTrackColor = SalatPrimary,
                            uncheckedThumbColor = SalatOutline, uncheckedTrackColor = SalatOutlineVariant,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.goal_log_clear), color = SalatError, fontFamily = InterFamily)
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                ) { Text(stringResource(R.string.goal_log_save), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

internal const val MAX_LOG_AMOUNT = 9999
