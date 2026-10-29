package com.salattracker.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatInverseOnSurface
import com.salattracker.core.theme.SalatInverseSurface
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.theme.SalatSurface
import com.salattracker.core.ui.LocalUse24HourFormat
import java.time.LocalTime

@Composable
fun AddGoalScreen(
    uiState: AddGoalUiState,
    actions: GoalFormActions,
    onCreateClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
    GoalFormScreen(
        form = uiState.form,
        isLoading = false,
        isSaving = uiState.isSaving,
        saveSuccess = uiState.saveSuccess,
        errorRes = uiState.errorRes,
        actions = actions,
        showTemplates = true,
        buttonText = stringResource(R.string.addgoal_create),
        onSaveClick = onCreateClick,
        onErrorDismiss = onErrorDismiss,
        onClearSaveSuccess = onClearSaveSuccess,
        testTag = "goal-new-screen",
    )
}

@Composable
internal fun GoalFormScreen(
    form: GoalFormState,
    isLoading: Boolean,
    isSaving: Boolean,
    saveSuccess: Boolean,
    errorRes: Int?,
    actions: GoalFormActions,
    showTemplates: Boolean,
    buttonText: String,
    onSaveClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
    testTag: String,
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SalatPrimary) }
        return
    }
    Box(Modifier.fillMaxSize().testTag(testTag)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            GoalFormFields(form, actions, LocalUse24HourFormat.current, showTemplates)
        }
        com.salattracker.core.ui.SalatPrimaryButton(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).testTag("goal-form-save"),
        ) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
            } else {
                Text(buttonText, fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                if (showTemplates) {
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(18.dp))
                }
            }
        }
        errorRes?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
                action = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(it), fontFamily = AppUiFontFamily) }
        }
        LaunchedEffect(saveSuccess) {
            if (saveSuccess) onClearSaveSuccess()
        }
    }
}

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
        Surface(shape = RoundedCornerShape(24.dp), color = SalatSurface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = AppUiFontFamily) }
                    TextButton(onClick = { onTimePicked(LocalTime.of(state.hour, state.minute)) }) {
                        Text(stringResource(R.string.action_ok), color = SalatPrimary, fontFamily = AppUiFontFamily)
                    }
                }
            }
        }
    }
}

internal fun previewFormActions() = GoalFormActions(
    onNameChanged = {}, onCategoryChanged = {}, onFrequencyChanged = {}, onTargetAmountChanged = {},
    onUnitChanged = {}, onDayToggled = {}, onEveryDaySelected = {}, onReminderEnabledChanged = {},
    onReminderTimeChanged = {}, onEndsAfterChanged = {}, onDurationDaysChanged = {},
    onUsesTasbihCounterChanged = {}, onTemplateSelected = { _, _ -> },
)
