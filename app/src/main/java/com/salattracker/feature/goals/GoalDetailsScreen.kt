package com.salattracker.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatInverseOnSurface
import com.salattracker.core.theme.SalatInverseSurface
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.theme.SalatStatusExcused
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.core.ui.formatAppTime
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DetailGold = Color(0xFFD9A441)
private val DetailPartial = Color(0xFF6DBBB6)

@Composable
fun GoalDetailsScreen(
    uiState: GoalDetailsUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    val goal = uiState.goal
    Box(Modifier.fillMaxSize().testTag("goal-detail-screen")) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = SalatPrimary)
            goal == null -> Unit
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            ) {
                GoalScheduleSummary(goal)
                GoalStatsRow(goal, uiState)
                GoalMonthHeader(
                    month = uiState.visibleMonth,
                    canGoPrev = uiState.canGoPrevMonth,
                    canGoNext = uiState.canGoNextMonth,
                    onPrev = onPrevMonth,
                    onNext = onNextMonth,
                )
                GoalMonthGrid(goal, uiState.cells, onDayClick)
                GoalLegend()
                Text(
                    stringResource(R.string.goals_v2_calendar_hint),
                    color = SalatOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
        }
    }
    if (uiState.showDeleteConfirm && goal != null) {
        DeleteGoalConfirmDialog(goal.name, onConfirmDelete, onDismissDeleteConfirm)
    }
}

@Composable
private fun GoalScheduleSummary(goal: SpiritualGoal) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val frequency = when (goal.frequency) {
        GoalFrequency.DAILY -> stringResource(R.string.goals_v2_every_day)
        GoalFrequency.WEEKLY -> stringResource(R.string.goals_v2_every_week)
        GoalFrequency.MONTHLY -> stringResource(R.string.goals_v2_every_month)
    }
    val reminder = goal.reminderTime?.takeIf { goal.reminderEnabled }?.formatAppTime(LocalUse24HourFormat.current)
    val parts = listOfNotNull(
        "${goal.targetAmount} ${goalUnitLabel(goal.unit)}",
        frequency,
        reminder?.let { stringResource(R.string.goals_v2_reminder_at, it) },
        stringResource(R.string.goals_v2_since, goal.startDate.format(formatter)),
    )
    Text(
        parts.joinToString(" · "),
        color = SalatOnSurfaceVariant,
        fontSize = 13.sp,
        fontFamily = AppUiFontFamily,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
}

@Composable
private fun GoalStatsRow(goal: SpiritualGoal, state: GoalDetailsUiState) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        GoalStat(
            value = stringResource(R.string.goals_v2_streak_value, state.streak.current),
            label = stringResource(R.string.goals_v2_streak_best, state.streak.best),
            icon = R.drawable.home_flame,
            modifier = Modifier.weight(1f),
        )
        GoalStat(
            value = stringResource(R.string.goals_v2_days_done_value, state.totals.periodsHit, state.totals.periodsCounted),
            label = stringResource(R.string.goals_v2_days_done),
            modifier = Modifier.weight(1f),
        )
        GoalStat(
            value = "${state.totals.loggedAmount} ${goalUnitLabel(goal.unit)}",
            label = stringResource(R.string.goals_v2_total),
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .55f))
}

@Composable
private fun GoalStat(value: String, label: String, modifier: Modifier, icon: Int? = null) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            icon?.let { Icon(painterResource(it), null, tint = DetailGold, modifier = Modifier.size(16.dp)) }
            Text(value, color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.Light, fontFamily = AppUiFontFamily)
        }
        Text(label, color = SalatOnSurfaceVariant, fontSize = 11.5.sp, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun GoalMonthHeader(month: YearMonth, canGoPrev: Boolean, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(painterResource(if (rtl) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left), null, tint = if (canGoPrev) SalatPrimary else SalatOutlineVariant)
        }
        Text(
            month.format(formatter),
            color = SalatOnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = AppTitleFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(painterResource(if (rtl) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right), null, tint = if (canGoNext) SalatPrimary else SalatOutlineVariant)
        }
    }
}

@Composable
private fun GoalMonthGrid(goal: SpiritualGoal, cells: List<GoalDayUi>, onDayClick: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(day.shortLabel(), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(4.dp))
    cells.chunked(7).filter { week -> week.any { it.inMonth } }.forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GoalMonthCell(day, goal.targetAmount, { onDayClick(day.date) })
                }
            }
        }
    }
}

@Composable
private fun GoalLegend() {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(SalatPrimary, R.string.goal_day_done)
        LegendDot(DetailPartial, R.string.goal_day_partial)
        LegendDot(SalatOutlineVariant, R.string.goal_day_missed)
        LegendDot(SalatStatusExcused, R.string.goal_day_excused)
    }
}

@Composable
private fun LegendDot(color: Color, labelRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(stringResource(labelRes), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily)
    }
}
