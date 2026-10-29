package com.salattracker.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.salattracker.R

@Composable
fun EditGoalScreen(
    uiState: EditGoalUiState,
    actions: GoalFormActions,
    onSaveClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
    GoalFormScreen(
        form = uiState.form,
        isLoading = uiState.isLoading,
        isSaving = uiState.isSaving,
        saveSuccess = uiState.saveSuccess,
        errorRes = uiState.errorRes,
        actions = actions,
        showTemplates = false,
        buttonText = stringResource(R.string.editgoal_save),
        onSaveClick = onSaveClick,
        onErrorDismiss = onErrorDismiss,
        onClearSaveSuccess = onClearSaveSuccess,
        testTag = "goal-edit-screen",
    )
}
