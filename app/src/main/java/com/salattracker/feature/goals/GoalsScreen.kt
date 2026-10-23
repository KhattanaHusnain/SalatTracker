package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
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
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalDayState
import com.salattracker.domain.usecase.GoalStatus
import java.time.LocalDate

// ── Goals Screen ──────────────────────────────────────────────────────────────
// Today summary, then one card per running goal with a 7-day strip (tap = log, long-press =
// edit that day), then a collapsible Archived section. Everything else lives on the details screen.

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onDayTap: (goalId: Long, date: LocalDate) -> Unit,
    onDayLongPress: (goalId: Long, date: LocalDate) -> Unit,
    onGoalClick: (Long) -> Unit,
    onAddGoalClick: () -> Unit,
    onTasbihClick: (Long) -> Unit,
    onLogAmountChanged: (Int) -> Unit,
    onLogSkippedChanged: (Boolean) -> Unit,
    onLogMarkDone: () -> Unit,
    onLogSave: () -> Unit,
    onLogClear: () -> Unit,
    onLogDismiss: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Nothing to show yet while loading — avoids flashing the empty state on first load.
            if (!uiState.isLoading) {
                if (!uiState.hasAnyGoals) {
                    EmptyGoalsCard(onAddGoalClick = onAddGoalClick)
                } else {
                    TodayCard(uiState.today)

                    if (uiState.active.isNotEmpty()) {
                        Text(
                            text       = stringResource(R.string.goals_active),
                            color      = SalatPrimary,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppTitleFontFamily,
                        )
                        uiState.active.forEach { card ->
                            GoalCard(
                                card = card,
                                onClick = { onGoalClick(card.goal.id) },
                                onDayTap = { date -> onDayTap(card.goal.id, date) },
                                onDayLongPress = { date -> onDayLongPress(card.goal.id, date) },
                                onTasbihClick = { onTasbihClick(card.goal.id) },
                            )
                        }
                    }

                    if (uiState.archived.isNotEmpty()) {
                        ArchivedSection(uiState.archived, onGoalClick)
                    }
                }
            }
        }

        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = AppUiFontFamily) }
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
}

// ── Today summary ─────────────────────────────────────────────────────────────

@Composable
private fun TodayCard(summary: TodaySummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (summary.dailyTotal > 0) {
                    Text(
                        text = stringResource(R.string.goals_done_today, summary.dailyDone, summary.dailyTotal),
                        color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.goals_nothing_due),
                        color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
                    )
                }
                if (summary.periodicTotal > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.goals_periodic_done, summary.periodicDone, summary.periodicTotal),
                        color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            val total = summary.dailyTotal
            val fraction = if (total > 0) summary.dailyDone.toFloat() / total else 0f
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f }, modifier = Modifier.fillMaxSize(),
                    color = SalatSecondaryContainer, strokeWidth = 6.dp, strokeCap = StrokeCap.Round,
                )
                CircularProgressIndicator(
                    progress = { fraction }, modifier = Modifier.fillMaxSize(),
                    color = SalatOnTertiaryContainer, strokeWidth = 6.dp, strokeCap = StrokeCap.Round,
                )
                Icon(painterResource(R.drawable.ic_auto_awesome), null, tint = SalatPrimaryContainer)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyGoalsCard(onAddGoalClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(R.drawable.ic_auto_awesome), null, tint = SalatOutline, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.goals_none_yet),
                color = SalatOnSurfaceVariant,
                fontFamily = AppUiFontFamily,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddGoalClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
            ) {
                Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_goal), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    card: GoalCardUi,
    onClick: () -> Unit,
    onDayTap: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    onTasbihClick: () -> Unit,
) {
    val goal = card.goal
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SalatPrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(goal.category.iconRes), null, tint = SalatOnPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(goal.name, color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cardSubtitle(card), color = SalatSecondary, fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (card.streak > 0) StreakBadge(card.streak)
            }
            Spacer(modifier = Modifier.height(14.dp))
            SevenDayStrip(
                days = card.strip,
                target = goal.targetAmount,
                onDayClick = onDayTap,
                onDayLongClick = onDayLongPress,
            )
            if (goal.usesTasbihCounter && goal.category == GoalCategory.DHIKR && goal.unit == "Time(s)") {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onTasbihClick, modifier = Modifier.align(Alignment.End)) {
                    Icon(painterResource(R.drawable.ic_auto_awesome), null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.tasbih_count_dhikr), fontFamily = AppUiFontFamily)
                }
            }
        }
    }
}

@Composable
private fun cardSubtitle(card: GoalCardUi): String {
    val goal = card.goal
    val base = when {
        goal.frequency == GoalFrequency.DAILY && card.skippedToday -> stringResource(R.string.goal_day_excused)
        goal.frequency == GoalFrequency.DAILY && !card.dueToday    -> stringResource(R.string.goal_not_due_today)
        goal.frequency == GoalFrequency.DAILY   -> stringResource(R.string.goal_today_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
        goal.frequency == GoalFrequency.WEEKLY  -> stringResource(R.string.goal_week_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
        else                                    -> stringResource(R.string.goal_month_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
    }
    val day = card.dayIndex
    return if (day != null) base + " · " + stringResource(R.string.goal_day_of, day, goal.durationDays) else base
}

// ── Archived section ──────────────────────────────────────────────────────────

@Composable
private fun ArchivedSection(archived: List<ArchivedGoalUi>, onGoalClick: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.goals_archived, archived.size),
                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
            )
            Icon(
                painterResource(R.drawable.ic_expand_more), null, tint = SalatOnSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                archived.forEach { item ->
                    Surface(
                        onClick = { onGoalClick(item.goal.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SalatSurfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(painterResource(item.goal.category.iconRes), null, tint = SalatSecondary, modifier = Modifier.size(20.dp))
                            Text(item.goal.name, color = SalatOnSurface, fontSize = 14.sp, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(if (item.status == GoalStatus.COMPLETED) R.string.goal_status_completed else R.string.goal_status_archived),
                                color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = AppUiFontFamily,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Delete confirmation dialog ─────────────────────────────────────────────────

@Composable
internal fun DeleteGoalConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SalatSurfaceContainerLowest,
        icon = {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(50)).background(SalatErrorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_delete), null, tint = SalatOnErrorContainer, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.goal_delete_title),
                color = SalatOnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppTitleFontFamily,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.goal_delete_desc),
                    color = SalatOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = AppUiFontFamily,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = SalatSurfaceContainerLow,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.goal_deleting),
                            color = SalatOnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = AppUiFontFamily,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = goalName,
                            color = SalatOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppUiFontFamily,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = SalatError, contentColor = SalatOnPrimary),
                shape   = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.action_delete), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
        },
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

private fun sampleStrip(states: List<GoalDayState>, amounts: List<Int>): List<GoalDayUi> {
    val today = LocalDate.now()
    return states.mapIndexed { i, state ->
        val date = today.minusDays((states.size - 1 - i).toLong())
        GoalDayUi(date, state, amounts[i], isToday = date == today)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GoalsScreenPreview() {
    val today = LocalDate.now()
    val quran = SpiritualGoal(1, "Read 5 Pages of Quran", GoalCategory.QURAN, GoalFrequency.DAILY, 5, "Pages", true, null, today.minusDays(20), 30, true)
    val fast = SpiritualGoal(2, "Fast Mon & Thu", GoalCategory.FASTING, GoalFrequency.WEEKLY, 2, "Time(s)", false, null, today.minusDays(20), 30, true)
    GoalsScreen(
        uiState = GoalsUiState(
            isLoading = false,
            today = TodaySummary(dailyDone = 1, dailyTotal = 2, periodicDone = 0, periodicTotal = 1),
            active = listOf(
                GoalCardUi(
                    goal = quran, streak = 12, currentAmount = 3, dueToday = true, skippedToday = false, dayIndex = null,
                    strip = sampleStrip(
                        listOf(GoalDayState.DONE, GoalDayState.DONE, GoalDayState.PARTIAL, GoalDayState.MISSED, GoalDayState.SKIPPED, GoalDayState.DONE, GoalDayState.PARTIAL),
                        listOf(5, 5, 2, 0, 0, 5, 3),
                    ),
                ),
                GoalCardUi(
                    goal = fast, streak = 4, currentAmount = 1, dueToday = true, skippedToday = false, dayIndex = null,
                    strip = sampleStrip(
                        listOf(GoalDayState.NONE, GoalDayState.LOGGED, GoalDayState.NONE, GoalDayState.NONE, GoalDayState.LOGGED, GoalDayState.NONE, GoalDayState.OPEN),
                        listOf(0, 1, 0, 0, 1, 0, 0),
                    ),
                ),
            ),
            archived = listOf(ArchivedGoalUi(quran.copy(id = 3, name = "Ramadan Quran Khatm", isActive = false), GoalStatus.ARCHIVED)),
        ),
        onDayTap = { _, _ -> }, onDayLongPress = { _, _ -> }, onGoalClick = {}, onAddGoalClick = {}, onTasbihClick = {},
        onLogAmountChanged = {}, onLogSkippedChanged = {}, onLogMarkDone = {}, onLogSave = {}, onLogClear = {},
        onLogDismiss = {}, onErrorDismiss = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GoalsScreenEmptyPreview() {
    GoalsScreen(
        uiState = GoalsUiState(isLoading = false),
        onDayTap = { _, _ -> }, onDayLongPress = { _, _ -> }, onGoalClick = {}, onAddGoalClick = {}, onTasbihClick = {},
        onLogAmountChanged = {}, onLogSkippedChanged = {}, onLogMarkDone = {}, onLogSave = {}, onLogClear = {},
        onLogDismiss = {}, onErrorDismiss = {},
    )
}
