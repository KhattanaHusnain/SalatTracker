package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.shortLabel
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.core.ui.formatAppTime
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalDayState
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.domain.usecase.GoalStreak
import com.salattracker.domain.usecase.GoalTotals
import com.salattracker.domain.usecase.monthGridDates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ── Goal details: header, streaks, totals, month calendar, actions ────────────

@Composable
fun GoalDetailsScreen(
    uiState: GoalDetailsUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onEditClick: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onLogAmountChanged: (Int) -> Unit,
    onLogSkippedChanged: (Boolean) -> Unit,
    onLogMarkDone: () -> Unit,
    onLogSave: () -> Unit,
    onLogClear: () -> Unit,
    onLogDismiss: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    val goal = uiState.goal
    Box(Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = SalatPrimary)
            goal == null      -> Unit   // deleted or unknown id — the Entry is about to navigate back
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                HeaderCard(goal = goal, status = uiState.status, dayIndex = uiState.dayIndex)
                StreakTiles(goal = goal, streak = uiState.streak)
                TotalsTiles(goal = goal, totals = uiState.totals)
                CalendarCard(
                    goal = goal,
                    month = uiState.visibleMonth,
                    cells = uiState.cells,
                    canGoPrev = uiState.canGoPrevMonth,
                    canGoNext = uiState.canGoNextMonth,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onDayClick = onDayClick,
                )
                ActionsRow(
                    status = uiState.status,
                    onEditClick = onEditClick,
                    onArchiveToggle = onArchiveToggle,
                    onDeleteClick = onDeleteClick,
                )
            }
        }

        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
        }
    }

    uiState.logSheet?.let { sheet ->
        LogDaySheet(
            state = sheet,
            onAmountChanged = onLogAmountChanged,
            onSkippedChanged = onLogSkippedChanged,
            onMarkDone = onLogMarkDone,
            onSave = onLogSave,
            onClear = onLogClear,
            onDismiss = onLogDismiss,
        )
    }

    if (uiState.showDeleteConfirm && goal != null) {
        DeleteGoalConfirmDialog(goalName = goal.name, onConfirm = onConfirmDelete, onDismiss = onDismissDeleteConfirm)
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(goal: SpiritualGoal, status: GoalStatus, dayIndex: Int?) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    FormCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SalatPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(goal.category.iconRes), null, tint = SalatOnPrimaryContainer) }
            Column(Modifier.weight(1f)) {
                Text(goal.name, color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(goal.category.label(), color = SalatSecondary, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            }
            when (status) {
                GoalStatus.COMPLETED -> StatusChip(stringResource(R.string.goal_status_completed))
                GoalStatus.ARCHIVED  -> StatusChip(stringResource(R.string.goal_status_archived))
                GoalStatus.ACTIVE    -> Unit
            }
        }
        Spacer(Modifier.height(16.dp))

        val schedule = buildString {
            append(goal.frequency.label())
        }
        val weekdays = if (goal.frequency == GoalFrequency.DAILY && goal.daysOfWeek.isNotEmpty()) {
            goal.daysOfWeek.sortedBy { it.value % 7 }.map { it.shortLabel() }.joinToString(", ")
        } else null
        DetailLine(stringResource(R.string.addgoal_target_freq), listOfNotNull(schedule, weekdays).joinToString(" · "))
        DetailLine(stringResource(R.string.addgoal_amount), "${goal.targetAmount} ${goalUnitLabel(goal.unit)}")
        DetailLine(
            stringResource(R.string.addgoal_reminder),
            goal.reminderTime?.takeIf { goal.reminderEnabled }?.formatAppTime(LocalUse24HourFormat.current) ?: stringResource(R.string.goal_no_reminder),
        )
        val end = goal.endDate
        val span = when {
            status == GoalStatus.COMPLETED && end != null -> stringResource(R.string.goal_completed_on, end.format(dateFormatter))
            end != null -> stringResource(R.string.goal_span, goal.startDate.format(dateFormatter), end.format(dateFormatter)) +
                (dayIndex?.let { " · " + stringResource(R.string.goal_day_of, it, goal.durationDays) } ?: "")
            else -> stringResource(R.string.goal_started_on, goal.startDate.format(dateFormatter)) + " · " + stringResource(R.string.goal_ongoing)
        }
        DetailLine(stringResource(R.string.goal_span_label), span)
    }
}

@Composable
private fun StatusChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(SalatTertiaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = SalatOnTertiaryContainer,  fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f))
        Text(value, color = SalatOnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
    }
}

// ── Streak + totals tiles ────────────────────────────────────────────────────

@Composable
private fun StreakTiles(goal: SpiritualGoal, streak: GoalStreak) {
    val unit = stringResource(
        when (goal.frequency) {
            GoalFrequency.DAILY   -> R.string.goal_unit_days
            GoalFrequency.WEEKLY  -> R.string.goal_unit_weeks
            GoalFrequency.MONTHLY -> R.string.goal_unit_months
        }
    )
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigTile(R.drawable.ic_local_fire_department, "${streak.current}", unit, stringResource(R.string.goal_current_streak), Modifier.weight(1f).fillMaxHeight())
        BigTile(R.drawable.ic_workspace_premium, "${streak.best}", unit, stringResource(R.string.goal_best_streak), Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun BigTile(iconRes: Int, value: String, unit: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SalatSurfaceContainerLowest,
        border = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(painterResource(iconRes), null, tint = SalatOnTertiaryContainer, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, color = SalatPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
                Text(unit, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily, modifier = Modifier.padding(bottom = 5.dp))
            }
            Text(label, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun TotalsTiles(goal: SpiritualGoal, totals: GoalTotals) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SmallTile("${totals.loggedAmount} ${goalUnitLabel(goal.unit)}", stringResource(R.string.goal_total_logged), Modifier.weight(1f).fillMaxHeight())
        SmallTile(
            if (totals.periodsCounted > 0) "${totals.periodsHit}/${totals.periodsCounted}" else "—",
            stringResource(R.string.stats_targets_hit),
            Modifier.weight(1f).fillMaxHeight(),
        )
        if (totals.excusedDays > 0) {
            SmallTile("${totals.excusedDays}", stringResource(R.string.goal_excused_count), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun SmallTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(SalatSurfaceContainer).padding(12.dp)) {
        Text(value, color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 2)
    }
}

// ── Month calendar ───────────────────────────────────────────────────────────

private val SUNDAY_FIRST = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
private fun CalendarCard(
    goal: SpiritualGoal,
    month: YearMonth,
    cells: List<GoalDayUi>,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    FormCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPrevMonth, enabled = canGoPrev) {
                Icon(
                    painterResource(if (rtl) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left),
                    stringResource(R.string.bulk_previous), tint = if (canGoPrev) SalatPrimary else SalatOutlineVariant,
                )
            }
            Text(month.format(monthFormatter), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            IconButton(onClick = onNextMonth, enabled = canGoNext) {
                Icon(
                    painterResource(if (rtl) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right),
                    stringResource(R.string.bulk_next), tint = if (canGoNext) SalatPrimary else SalatOutlineVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            SUNDAY_FIRST.forEach { day ->
                Text(
                    text = day.shortLabel(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = SalatOutline,  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // monthGridDates is always 6 weeks; the last row is hidden when it is entirely next month.
        val rows = cells.chunked(7).filter { week -> week.any { it.inMonth } }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            GoalDayCell(day = day, target = goal.targetAmount, size = 38.dp, onClick = { onDayClick(day.date) }, onLongClick = { onDayClick(day.date) })
                        }
                    }
                }
            }
        }
    }
}

// ── Actions ──────────────────────────────────────────────────────────────────

@Composable
private fun ActionsRow(status: GoalStatus, onEditClick: () -> Unit, onArchiveToggle: () -> Unit, onDeleteClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onEditClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
            ) {
                Icon(painterResource(R.drawable.ic_edit_square), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.goal_action_edit), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
            // A finished goal can't be archived/restored — editing it (extending the end) is how it comes back.
            if (status != GoalStatus.COMPLETED) {
                FilledTonalButton(
                    onClick = onArchiveToggle,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(if (status == GoalStatus.ARCHIVED) R.string.goal_action_restore else R.string.goal_action_archive),
                        fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        TextButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.editgoal_delete), color = SalatError, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GoalDetailsScreenPreview() {
    val today = LocalDate.now()
    val goal = SpiritualGoal(
        1, "Read 5 Pages of Quran", GoalCategory.QURAN, GoalFrequency.DAILY, 5, "Pages", true,
        LocalTime.of(4, 30), today.minusDays(40), 30, true, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    )
    val month = YearMonth.from(today)
    val cells = monthGridDates(month).map { date ->
        val state = when {
            date.isAfter(today) -> GoalDayState.LOCKED
            date.dayOfMonth % 5 == 0 -> GoalDayState.MISSED
            date.dayOfMonth % 4 == 0 -> GoalDayState.PARTIAL
            date.dayOfMonth % 7 == 0 -> GoalDayState.SKIPPED
            else -> GoalDayState.DONE
        }
        GoalDayUi(date, state, amount = if (state == GoalDayState.PARTIAL) 2 else 5, isToday = date == today, inMonth = YearMonth.from(date) == month)
    }
    GoalDetailsScreen(
        uiState = GoalDetailsUiState(
            isLoading = false, goal = goal, streak = GoalStreak(12, 21), totals = GoalTotals(142, 18, 24, 2),
            visibleMonth = month, canGoPrevMonth = true, canGoNextMonth = false, cells = cells,
        ),
        onPrevMonth = {}, onNextMonth = {}, onDayClick = {}, onEditClick = {}, onArchiveToggle = {},
        onDeleteClick = {}, onConfirmDelete = {}, onDismissDeleteConfirm = {}, onLogAmountChanged = {},
        onLogSkippedChanged = {}, onLogMarkDone = {}, onLogSave = {}, onLogClear = {}, onLogDismiss = {}, onErrorDismiss = {},
    )
}
