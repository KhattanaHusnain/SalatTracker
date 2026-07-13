package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.label
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalWithProgress
import com.salattracker.domain.model.SpiritualGoal
import java.time.LocalDate

// ── Goals Screen ──────────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onCheckIn: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            TodaysFocusCard(overallPercent = uiState.overallPercent)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = stringResource(R.string.goals_active),
                        color      = SalatPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MontserratFamily,
                    )
                    Text(
                        text       = stringResource(R.string.goals_in_progress, uiState.goals.size),
                        color      = SalatSecondary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFamily,
                    )
                }

                if (uiState.isLoading) {
                    // Nothing to show yet — avoid flashing an empty state during the first load.
                } else if (uiState.goals.isEmpty()) {
                    EmptyGoalsCard()
                } else {
                    uiState.goals.forEach { goalWithProgress ->
                        GoalCard(
                            goalWithProgress = goalWithProgress,
                            onCheckIn = { onCheckIn(goalWithProgress.goal.id) },
                            onEditClick = { onEditClick(goalWithProgress.goal.id) },
                            onDeleteClick = { onDeleteClick(goalWithProgress.goal.id) },
                        )
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
            ) { Text(msg, fontFamily = InterFamily) }
        }
    }

    uiState.deleteConfirmGoal?.let { goalWithProgress ->
        DeleteGoalConfirmDialog(
            goalName  = goalWithProgress.goal.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
        )
    }
}

// ── Today's Focus summary card ────────────────────────────────────────────────

@Composable
private fun TodaysFocusCard(overallPercent: Int) {
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
            Column {
                Text(stringResource(R.string.goals_todays_focus), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                Text(stringResource(R.string.goals_progress_glance), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$overallPercent%", color = SalatPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.goals_completed_caps),
                        color = SalatSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFamily,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress   = { 1f },
                    modifier   = Modifier.fillMaxSize(),
                    color      = SalatSecondaryContainer,
                    strokeWidth = 6.dp,
                    strokeCap  = StrokeCap.Round,
                )
                CircularProgressIndicator(
                    progress    = { overallPercent / 100f },
                    modifier    = Modifier.fillMaxSize(),
                    color       = SalatOnTertiaryContainer,
                    strokeWidth = 6.dp,
                    strokeCap   = StrokeCap.Round,
                )
                Icon(painterResource(R.drawable.ic_auto_awesome), null, tint = SalatPrimaryContainer)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyGoalsCard() {
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
                fontFamily = InterFamily,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goalWithProgress: GoalWithProgress,
    onCheckIn: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val goal = goalWithProgress.goal

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SalatPrimaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(goal.category.iconRes), null, tint = SalatOnPrimaryContainer)
                    }
                    Column {
                        Text(goal.name, color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                        Text(goal.category.label(), color = SalatSecondary, fontSize = 11.sp, fontFamily = InterFamily)
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(SalatTertiaryContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = goal.frequency.label().uppercase(),
                        color = SalatOnTertiaryContainer,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFamily,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.goals_progress), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                Text(
                    text = "${goalWithProgress.currentAmount}/${goal.targetAmount} ${goal.unit}",
                    color = SalatOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { goalWithProgress.progressFraction },
                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color      = SalatPrimaryContainer,
                trackColor = SalatSurfaceContainerHigh,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick  = onCheckIn,
                    enabled  = !goalWithProgress.isComplete,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SalatPrimary,
                        contentColor   = SalatOnPrimary,
                        disabledContainerColor = SalatSurfaceContainerHigh,
                        disabledContentColor   = SalatOnSurfaceVariant,
                    ),
                ) {
                    Icon(
                        painter = painterResource(if (goalWithProgress.isComplete) R.drawable.ic_check_circle_filled else R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (goalWithProgress.isComplete) R.string.goal_completed else if (goal.targetAmount > 1) R.string.goal_checkin else R.string.action_mark_done),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                IconButton(
                    onClick  = onEditClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SalatSurfaceContainerHigh),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_square),
                        contentDescription = stringResource(R.string.cd_edit_goal),
                        tint = SalatOnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick  = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SalatErrorContainer),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.cd_delete_goal),
                        tint = SalatOnErrorContainer,
                        modifier = Modifier.size(20.dp),
                    )
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
                fontFamily = MontserratFamily,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.goal_delete_desc),
                    color = SalatOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = InterFamily,
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
                            fontFamily = InterFamily,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = goalName,
                            color = SalatOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
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
            ) { Text(stringResource(R.string.action_delete), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = InterFamily) }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val GoalCategory.iconRes get() = when (this) {
    GoalCategory.SALAT   -> R.drawable.ic_mosque
    GoalCategory.QURAN   -> R.drawable.ic_history_edu
    GoalCategory.FASTING -> R.drawable.ic_schedule
    GoalCategory.CHARITY -> R.drawable.ic_workspace_premium
    GoalCategory.DHIKR   -> R.drawable.ic_local_fire_department
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GoalsScreenPreview() {
    GoalsScreen(
        uiState = GoalsUiState(
            isLoading = false,
            overallPercent = 68,
            goals = listOf(
                GoalWithProgress(
                    goal = SpiritualGoal(1, "Read 5 Pages of Quran", GoalCategory.QURAN, GoalFrequency.DAILY, 5, "Pages", true, null, LocalDate.now(), 30, true),
                    currentAmount = 3,
                ),
                GoalWithProgress(
                    goal = SpiritualGoal(2, "Morning Adhkar", GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)", true, null, LocalDate.now(), 30, true),
                    currentAmount = 0,
                ),
                GoalWithProgress(
                    goal = SpiritualGoal(3, "Fast on Mondays", GoalCategory.FASTING, GoalFrequency.WEEKLY, 4, "Time(s)", false, null, LocalDate.now(), 30, true),
                    currentAmount = 2,
                ),
            ),
        ),
        onCheckIn             = {},
        onEditClick           = {},
        onDeleteClick         = {},
        onConfirmDelete       = {},
        onDismissDeleteConfirm = {},
        onErrorDismiss        = {},
    )
}
