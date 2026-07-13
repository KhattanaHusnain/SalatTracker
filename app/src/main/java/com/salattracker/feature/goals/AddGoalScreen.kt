package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.label
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Add Goal Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(
    uiState: AddGoalUiState,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (GoalCategory) -> Unit,
    onFrequencyChanged: (GoalFrequency) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
    onDurationDaysChanged: (String) -> Unit,
    onCreateClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
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
        }

        // ── Create button (fixed at bottom) ────────────────────────────────────
        Button(
            onClick  = onCreateClick,
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
                Text(stringResource(R.string.addgoal_create), fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(20.dp))
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
            ) { Text(stringResource(R.string.addgoal_created), fontFamily = InterFamily) }
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
}

// ── Shared form card ──────────────────────────────────────────────────────────

@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = SalatPrimary,
    unfocusedBorderColor = SalatOutlineVariant,
    focusedContainerColor   = SalatSurfaceContainerLow,
    unfocusedContainerColor = SalatSurfaceContainerLow,
)

// ── Category selector ─────────────────────────────────────────────────────────

@Composable
internal fun CategoryOption(category: GoalCategory, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape   = CircleShape,
            color   = if (isSelected) SalatPrimaryContainer else Color.Transparent,
            border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(category.iconResPublic),
                    contentDescription = null,
                    tint = if (isSelected) SalatOnPrimaryContainer else SalatSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.label(),
            color = if (isSelected) SalatPrimary else SalatSecondary,
            fontSize = 10.sp,
            fontFamily = InterFamily,
        )
    }
}

private val GoalCategory.iconResPublic get() = when (this) {
    GoalCategory.SALAT   -> R.drawable.ic_mosque
    GoalCategory.QURAN   -> R.drawable.ic_history_edu
    GoalCategory.FASTING -> R.drawable.ic_schedule
    GoalCategory.CHARITY -> R.drawable.ic_workspace_premium
    GoalCategory.DHIKR   -> R.drawable.ic_local_fire_department
}

// ── Frequency tabs ────────────────────────────────────────────────────────────

@Composable
internal fun FrequencyTabs(selected: GoalFrequency, onSelected: (GoalFrequency) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(SalatSurfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GoalFrequency.values().forEach { freq ->
            val isSelected = freq == selected
            Surface(
                onClick  = { onSelected(freq) },
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(50),
                color    = if (isSelected) SalatPrimaryContainer else Color.Transparent,
            ) {
                Text(
                    text       = freq.label(),
                    modifier   = Modifier.padding(vertical = 8.dp),
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    color      = if (isSelected) SalatOnPrimaryContainer else SalatSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                )
            }
        }
    }
}

// ── Unit dropdown ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GOAL_UNITS.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, fontFamily = InterFamily) },
                    onClick = { onSelected(unit); expanded = false },
                )
            }
        }
    }
}

// ── Time picker modal ─────────────────────────────────────────────────────────

internal val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerModal(
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = SalatSurfaceContainerLowest) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = InterFamily) }
                    TextButton(onClick = { onTimePicked(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_ok), fontFamily = InterFamily, color = SalatPrimary)
                    }
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddGoalScreenPreview() {
    AddGoalScreen(
        uiState = AddGoalUiState(),
        onNameChanged = {},
        onCategoryChanged = {},
        onFrequencyChanged = {},
        onTargetAmountChanged = {},
        onUnitChanged = {},
        onReminderEnabledChanged = {},
        onReminderTimeChanged = {},
        onDurationDaysChanged = {},
        onCreateClick = {},
        onErrorDismiss = {},
        onClearSaveSuccess = {},
    )
}
