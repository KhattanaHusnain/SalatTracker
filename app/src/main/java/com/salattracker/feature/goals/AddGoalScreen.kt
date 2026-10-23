package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import java.time.LocalTime

// ── Add Goal Screen ───────────────────────────────────────────────────────────

@Composable
fun AddGoalScreen(
    uiState: AddGoalUiState,
    actions: GoalFormActions,
    onCreateClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
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
            GoalFormFields(form = uiState.form, actions = actions, use24HourFormat = LocalUse24HourFormat.current)
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
                Text(stringResource(R.string.addgoal_create), fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            ) { Text(msg, fontFamily = AppUiFontFamily) }
        }
        if (uiState.saveSuccess) {
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                action         = { TextButton(onClick = onClearSaveSuccess) { Text(stringResource(R.string.action_ok), color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) { Text(stringResource(R.string.addgoal_created), fontFamily = AppUiFontFamily) }
        }
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
                    painter = painterResource(category.iconRes),
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
            fontFamily = AppUiFontFamily,
        )
    }
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
                    fontFamily = AppUiFontFamily,
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
            value = goalUnitLabel(selected),
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
                    text = { Text(goalUnitLabel(unit), fontFamily = AppUiFontFamily) },
                    onClick = { onSelected(unit); expanded = false },
                )
            }
        }
    }
}

// ── Time picker modal ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerModal(
    initialTime: LocalTime,
    use24HourFormat: Boolean,
    onTimePicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = use24HourFormat)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = SalatSurfaceContainerLowest) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = AppUiFontFamily) }
                    TextButton(onClick = { onTimePicked(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_ok), fontFamily = AppUiFontFamily, color = SalatPrimary)
                    }
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

internal fun previewFormActions() = GoalFormActions(
    onNameChanged = {}, onCategoryChanged = {}, onFrequencyChanged = {}, onTargetAmountChanged = {},
    onUnitChanged = {}, onDayToggled = {}, onEveryDaySelected = {}, onReminderEnabledChanged = {}, onUsesTasbihCounterChanged = {},
    onReminderTimeChanged = {}, onEndsAfterChanged = {}, onDurationDaysChanged = {},
    onTemplateSelected = { _, _ -> },
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddGoalScreenPreview() {
    AddGoalScreen(
        uiState = AddGoalUiState(),
        actions = previewFormActions(),
        onCreateClick = {},
        onErrorDismiss = {},
        onClearSaveSuccess = {},
    )
}
