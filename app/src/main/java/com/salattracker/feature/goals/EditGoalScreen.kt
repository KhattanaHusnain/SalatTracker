package com.salattracker.feature.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import java.time.LocalTime

// ── Edit Goal Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalScreen(
    uiState: EditGoalUiState,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (GoalCategory) -> Unit,
    onFrequencyChanged: (GoalFrequency) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
    onDurationDaysChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SalatPrimary)
        }
        return
    }

    var showTimePicker by remember { mutableStateOf(false) }

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

            // ── Goal name ─────────────────────────────────────────────────────
            FormCard {
                Text(stringResource(R.string.addgoal_name), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value         = uiState.name,
                    onValueChange = onNameChanged,
                    placeholder   = { Text(stringResource(R.string.addgoal_name_hint), fontFamily = InterFamily) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = outlinedFieldColors(),
                    shape         = RoundedCornerShape(8.dp),
                )
            }

            // ── Category ──────────────────────────────────────────────────────
            FormCard {
                Text(stringResource(R.string.addgoal_category), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GoalCategory.values().forEach { category ->
                        CategoryOption(
                            category   = category,
                            isSelected = category == uiState.category,
                            onClick    = { onCategoryChanged(category) },
                        )
                    }
                }
            }

            // ── Frequency + target ────────────────────────────────────────────
            FormCard {
                Text(stringResource(R.string.addgoal_target_freq), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                Spacer(modifier = Modifier.height(12.dp))
                FrequencyTabs(selected = uiState.frequency, onSelected = onFrequencyChanged)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.addgoal_amount), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.targetAmount,
                            onValueChange = onTargetAmountChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = outlinedFieldColors(),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.addgoal_unit), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                        Spacer(modifier = Modifier.height(6.dp))
                        UnitDropdown(selected = uiState.unit, onSelected = onUnitChanged)
                    }
                }
            }

            // ── Reminder ──────────────────────────────────────────────────────
            FormCard {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.ic_schedule), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.addgoal_reminder), color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                    }
                    Switch(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = onReminderEnabledChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SalatOnPrimary,
                            checkedTrackColor = SalatPrimary,
                            uncheckedThumbColor = SalatOutline,
                            uncheckedTrackColor = SalatOutlineVariant,
                        ),
                    )
                }
                if (uiState.reminderEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.addgoal_notify_at), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                        Surface(
                            onClick = { showTimePicker = true },
                            shape   = RoundedCornerShape(8.dp),
                            color   = SalatSurfaceContainerLow,
                        ) {
                            Text(
                                text = uiState.reminderTime.format(timeFormatter),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = SalatPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = MontserratFamily,
                            )
                        }
                    }
                }
            }

            // ── Duration ──────────────────────────────────────────────────────
            FormCard {
                Text(stringResource(R.string.addgoal_duration), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.durationDays,
                    onValueChange = onDurationDaysChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                )
            }

            // ── Delete goal link ──────────────────────────────────────────────
            TextButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.editgoal_delete), color = SalatError, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
            }
        }

        // ── Save button (fixed at bottom) ───────────────────────────────────────
        Button(
            onClick  = onSaveClick,
            enabled  = !uiState.isSaving,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp)
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SalatPrimaryContainer, contentColor = SalatOnPrimary),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
            } else {
                Text(stringResource(R.string.editgoal_save), fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }
        if (uiState.saveSuccess) {
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                action         = { TextButton(onClick = onClearSaveSuccess) { Text(stringResource(R.string.action_ok), color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) { Text(stringResource(R.string.editgoal_updated), fontFamily = InterFamily) }
        }
    }

    if (showTimePicker) {
        TimePickerModal(
            initialTime = uiState.reminderTime,
            onTimePicked = { picked ->
                onReminderTimeChanged(picked)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (uiState.showDeleteConfirm) {
        DeleteGoalConfirmDialog(
            goalName  = uiState.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditGoalScreenPreview() {
    EditGoalScreen(
        uiState = EditGoalUiState(isLoading = false, name = "Tahajjud Prayer"),
        onNameChanged = {},
        onCategoryChanged = {},
        onFrequencyChanged = {},
        onTargetAmountChanged = {},
        onUnitChanged = {},
        onReminderEnabledChanged = {},
        onReminderTimeChanged = {},
        onDurationDaysChanged = {},
        onSaveClick = {},
        onDeleteClick = {},
        onConfirmDelete = {},
        onDismissDeleteConfirm = {},
        onErrorDismiss = {},
        onClearSaveSuccess = {},
    )
}
