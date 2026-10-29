package com.salattracker.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatErrorContainer
import com.salattracker.core.theme.SalatInverseOnSurface
import com.salattracker.core.theme.SalatInverseSurface
import com.salattracker.core.theme.SalatOnErrorContainer
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.usecase.GoalStatus
import java.time.LocalDate

private val GoalsGold = Color(0xFFD9A441)

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onGoalClick: (Long) -> Unit,
    onAddGoalClick: (GoalTemplate?) -> Unit,
    onDayClick: (Long, LocalDate) -> Unit,
    onQuickAdd: (Long) -> Unit,
    onTasbihClick: (Long) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag("goals-v2-screen")) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = SalatPrimary)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                if (!uiState.hasAnyGoals) {
                    EmptyGoals(onAddGoalClick)
                } else {
                    GoalsHero(uiState.today)
                    uiState.active.forEach { card ->
                        GoalRow(
                            card = card,
                            onGoalClick = { onGoalClick(card.goal.id) },
                            onDayClick = { onDayClick(card.goal.id, it) },
                            onQuickAdd = { onQuickAdd(card.goal.id) },
                            onTasbihClick = { onTasbihClick(card.goal.id) },
                        )
                    }
                    if (uiState.archived.isNotEmpty()) {
                        ArchivedGoals(uiState.archived, onGoalClick)
                    }
                }
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
}

@Composable
private fun EmptyGoals(onAdd: (GoalTemplate?) -> Unit) {
    Column(Modifier.testTag("goals-empty")) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.goals_v2_empty_intro),
            color = SalatOnSurfaceVariant,
            fontSize = 14.sp,
            fontFamily = AppUiFontFamily,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            GoalTemplate.READ_QURAN,
            GoalTemplate.MORNING_ADHKAR,
            GoalTemplate.TAHAJJUD,
            GoalTemplate.FAST_MON_THU,
            GoalTemplate.WEEKLY_SADAQAH,
        ).forEach { template ->
            SuggestionRow(
                name = stringResource(template.nameRes),
                summary = template.summary(),
                iconRes = template.category.iconRes,
                onAdd = { onAdd(template) },
            )
        }
        SuggestionRow(
            name = stringResource(R.string.goals_v2_custom_goal),
            summary = null,
            iconRes = R.drawable.ic_add,
            onAdd = { onAdd(null) },
        )
    }
}

@Composable
private fun GoalTemplate.summary(): String = when (this) {
    GoalTemplate.READ_QURAN -> stringResource(R.string.goals_v2_preset_quran_summary)
    GoalTemplate.MORNING_ADHKAR -> stringResource(R.string.goals_v2_preset_daily_once)
    GoalTemplate.TAHAJJUD -> stringResource(R.string.goals_v2_preset_tahajjud_summary)
    GoalTemplate.FAST_MON_THU -> stringResource(R.string.goals_v2_preset_fast_summary)
    GoalTemplate.WEEKLY_SADAQAH -> stringResource(R.string.goals_v2_preset_weekly_once)
    else -> stringResource(R.string.goals_v2_more_options)
}

@Composable
private fun SuggestionRow(name: String, summary: String?, iconRes: Int, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(74.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(SalatPrimaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
            summary?.let { Text(it, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily) }
        }
        TextButton(onClick = onAdd, modifier = Modifier.height(44.dp)) {
            Text(stringResource(R.string.goals_v2_add), color = SalatPrimary, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
        }
    }
    HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .55f))
}

@Composable
private fun GoalsHero(summary: TodaySummary) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp).testTag("goals-hero")) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (summary.dailyTotal == 0) "0" else stringResource(R.string.goals_v2_done_count, summary.dailyDone, summary.dailyTotal),
                color = SalatPrimary,
                fontSize = 50.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppTitleFontFamily,
            )
            Text(
                stringResource(if (summary.dailyTotal == 0) R.string.goals_nothing_due else R.string.goals_v2_done_today),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        val fraction = if (summary.dailyTotal > 0) summary.dailyDone.toFloat() / summary.dailyTotal else 0f
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(SalatSecondaryContainer)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(GoalsGold))
        }
    }
    HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .55f))
}

@Composable
private fun GoalRow(
    card: GoalCardUi,
    onGoalClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onQuickAdd: () -> Unit,
    onTasbihClick: () -> Unit,
) {
    val goal = card.goal
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("goal-row-${goal.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(SalatPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(goal.category.iconRes), null, tint = SalatPrimary, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f).clickable(onClick = onGoalClick)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.name,
                        color = SalatOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AppUiFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (card.streak > 0) StreakBadge(card.streak, Modifier.padding(start = 6.dp))
                }
                Text(goalProgress(card), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            }
            GoalQuickButton(card, onQuickAdd)
        }
        Spacer(Modifier.height(10.dp))
        GoalWeekStrip(card.strip, onDayClick)
        if (goal.usesTasbihCounter) {
            TextButton(onClick = onTasbihClick, modifier = Modifier.align(Alignment.End).height(40.dp)) {
                Text(stringResource(R.string.tasbih_count_dhikr), color = SalatPrimary, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            }
        }
    }
    HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .55f))
}

@Composable
private fun goalProgress(card: GoalCardUi): String {
    val goal = card.goal
    return when {
        goal.frequency == GoalFrequency.DAILY && card.skippedToday -> stringResource(R.string.goal_day_excused)
        goal.frequency == GoalFrequency.DAILY && !card.dueToday -> stringResource(R.string.goal_not_due_today)
        goal.frequency == GoalFrequency.DAILY && card.isPeriodDone -> stringResource(R.string.goals_v2_done_today)
        goal.frequency == GoalFrequency.DAILY -> stringResource(R.string.goal_today_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
        goal.frequency == GoalFrequency.WEEKLY -> stringResource(R.string.goal_week_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
        else -> stringResource(R.string.goal_month_progress, card.currentAmount, goal.targetAmount, goalUnitLabel(goal.unit))
    }
}

@Composable
private fun GoalQuickButton(card: GoalCardUi, onClick: () -> Unit) {
    val complete = card.isPeriodDone
    val enabled = card.dueToday && !card.skippedToday && !complete
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(if (complete) SalatPrimaryContainer else SalatPrimary),
    ) {
        Icon(
            painterResource(if (complete || card.goal.targetAmount == 1) R.drawable.ic_check else R.drawable.ic_add),
            contentDescription = stringResource(if (complete) R.string.goal_day_done else R.string.goals_v2_add_progress),
            tint = if (complete) SalatPrimary else if (enabled) SalatOnPrimary else SalatOutlineVariant,
        )
    }
}

@Composable
private fun ArchivedGoals(items: List<ArchivedGoalUi>, onGoalClick: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.testTag("goals-archived")) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.goals_archived, items.size),
                color = SalatPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AppUiFontFamily,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_chevron_right), null, tint = SalatPrimary)
        }
        if (expanded) items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onGoalClick(item.goal.id) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(item.goal.category.iconRes), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(item.goal.name, color = SalatOnSurface, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    stringResource(if (item.status == GoalStatus.COMPLETED) R.string.goal_status_completed else R.string.goal_status_archived),
                    color = SalatOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
            }
            HorizontalDivider(color = SalatOutlineVariant.copy(alpha = .4f))
        }
    }
}

@Composable
internal fun DeleteGoalConfirmDialog(goalName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(Modifier.size(52.dp).clip(CircleShape).background(SalatErrorContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_delete), null, tint = SalatOnErrorContainer)
            }
        },
        title = { Text(stringResource(R.string.goal_delete_title), fontFamily = AppTitleFontFamily) },
        text = {
            Text(
                stringResource(R.string.goal_delete_desc) + "\n\n" + goalName,
                color = SalatOnSurfaceVariant,
                fontFamily = AppUiFontFamily,
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = SalatError)) {
                Text(stringResource(R.string.action_delete), fontFamily = AppUiFontFamily)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = AppUiFontFamily) } },
    )
}
