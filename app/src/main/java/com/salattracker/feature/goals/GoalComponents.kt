package com.salattracker.feature.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatStatusExcused
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.usecase.GoalDayState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GoalGold = Color(0xFFD9A441)
private val GoalPartial = Color(0xFF6DBBB6)

data class GoalDayUi(
    val date: LocalDate,
    val state: GoalDayState,
    val amount: Int,
    val isToday: Boolean = false,
    val inMonth: Boolean = true,
)

@Composable
internal fun rememberDayLabelFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }
}

private fun GoalDayState.labelRes(): Int = when (this) {
    GoalDayState.DONE -> R.string.goal_day_done
    GoalDayState.PARTIAL -> R.string.goal_day_partial
    GoalDayState.MISSED -> R.string.goal_day_missed
    GoalDayState.SKIPPED -> R.string.goal_day_excused
    GoalDayState.OPEN -> R.string.goal_day_open
    GoalDayState.NOT_DUE -> R.string.goal_day_not_due
    GoalDayState.LOGGED -> R.string.goal_day_logged
    GoalDayState.NONE -> R.string.goal_day_none
    GoalDayState.LOCKED -> R.string.goal_day_locked
}

@Composable
fun StreakBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_local_fire_department),
            contentDescription = null,
            tint = GoalGold,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "$count",
            color = SalatOnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
    }
}

@Composable
fun GoalWeekStrip(
    days: List<GoalDayUi>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val enabled = day.state != GoalDayState.LOCKED && day.state != GoalDayState.NOT_DUE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = day.date.dayOfWeek.shortLabel(),
                    color = if (day.isToday) GoalGold else SalatOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = AppUiFontFamily,
                    maxLines = 1,
                )
                GoalWeekDot(day = day, enabled = enabled, onClick = { onDayClick(day.date) })
            }
        }
    }
}

@Composable
private fun GoalWeekDot(day: GoalDayUi, enabled: Boolean, onClick: () -> Unit) {
    val color = when (day.state) {
        GoalDayState.DONE, GoalDayState.LOGGED -> SalatPrimary
        GoalDayState.PARTIAL -> GoalPartial
        GoalDayState.SKIPPED -> SalatStatusExcused
        GoalDayState.MISSED -> SalatOutlineVariant
        GoalDayState.OPEN -> SalatOutlineVariant
        else -> Color.Transparent
    }
    val muted = SalatOutlineVariant
    val description = stringResource(
        R.string.goal_day_cd,
        day.date.format(rememberDayLabelFormatter()),
        stringResource(day.state.labelRes()),
    )
    Box(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = description }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (day.state == GoalDayState.NOT_DUE) {
                drawLine(
                    color = muted,
                    start = Offset(size.width * .34f, size.height / 2),
                    end = Offset(size.width * .66f, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else if (color != Color.Transparent) {
                drawCircle(color = color, radius = size.minDimension * .42f)
            }
            if (day.isToday) {
                drawCircle(
                    color = GoalGold,
                    radius = size.minDimension * .48f,
                    style = Stroke(width = 1.6.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun GoalMonthCell(
    day: GoalDayUi,
    target: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val enabled = day.state != GoalDayState.LOCKED && day.state != GoalDayState.NOT_DUE
    val description = stringResource(
        R.string.goal_day_cd,
        day.date.format(rememberDayLabelFormatter()),
        stringResource(day.state.labelRes()),
    )
    val ring = when (day.state) {
        GoalDayState.DONE -> SalatPrimary
        GoalDayState.PARTIAL, GoalDayState.LOGGED -> GoalPartial
        GoalDayState.SKIPPED -> SalatStatusExcused
        GoalDayState.MISSED, GoalDayState.OPEN -> SalatOutlineVariant
        else -> Color.Transparent
    }
    val track = SalatSecondaryContainer
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (day.inMonth) 1f else .35f)
            .drawBehind {
                if (day.isToday) {
                    drawCircle(GoalGold.copy(alpha = .14f), radius = this.size.minDimension * .58f)
                }
            }
            .semantics { contentDescription = description }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (ring != Color.Transparent) {
                val fraction = if (day.state == GoalDayState.PARTIAL && target > 0) {
                    (day.amount.toFloat() / target).coerceIn(0f, 1f)
                } else 1f
                val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Butt)
                val inset = 4.dp.toPx()
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                if (fraction < 1f) {
                    drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
                }
                drawArc(ring, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = stroke)
            }
            if (day.isToday) {
                drawCircle(
                    GoalGold,
                    radius = this.size.minDimension * .49f,
                    style = Stroke(width = 1.6.dp.toPx()),
                )
            }
        }
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                day.isToday -> GoalGold
                day.state == GoalDayState.LOCKED || !day.inMonth -> SalatOutlineVariant
                day.state == GoalDayState.SKIPPED -> SalatStatusExcused
                day.state == GoalDayState.MISSED -> SalatError
                else -> SalatOnSurface
            },
            fontSize = 12.5.sp,
            fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
    }
}
