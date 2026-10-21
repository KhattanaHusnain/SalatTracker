package com.salattracker.feature.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*

// ── Edit Goal Screen ──────────────────────────────────────────────────────────
// Same fields as Add Goal (shared GoalFormFields). Deleting a goal lives on its details screen.

@Composable
fun EditGoalScreen(
    uiState: EditGoalUiState,
    actions: GoalFormActions,
    onSaveClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onClearSaveSuccess: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SalatPrimary)
        }
        return
    }

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
            GoalFormFields(form = uiState.form, actions = actions)
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
                Text(stringResource(R.string.editgoal_save), fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            ) { Text(stringResource(R.string.editgoal_updated), fontFamily = AppUiFontFamily) }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditGoalScreenPreview() {
    EditGoalScreen(
        uiState = EditGoalUiState(isLoading = false, form = GoalFormState(name = "Tahajjud Prayer")),
        actions = previewFormActions().copy(onTemplateSelected = null),
        onSaveClick = {},
        onErrorDismiss = {},
        onClearSaveSuccess = {},
    )
}
