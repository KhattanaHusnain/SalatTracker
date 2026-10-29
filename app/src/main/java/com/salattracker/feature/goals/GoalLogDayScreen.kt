package com.salattracker.feature.goals

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatInverseOnSurface
import com.salattracker.core.theme.SalatInverseSurface
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.salatSwitchColors
import java.time.format.DateTimeFormatter

@Composable
fun GoalLogDayScreen(
    state: GoalLogDayUiState,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onMarkDone: () -> Unit,
    onSkippedChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag("goal-log-day-screen")) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = SalatPrimary)
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val locale = LocalConfiguration.current.locales[0]
                val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }
                Spacer(Modifier.height(28.dp))
                Text(state.goalName, color = SalatOnSurface, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center)
                Text(state.date.format(formatter), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
                Spacer(Modifier.height(26.dp))

                if (state.target == 1) {
                    Button(
                        onClick = onMarkDone,
                        enabled = !state.skipped && state.amount < 1,
                        colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                    ) {
                        Text(
                            if (state.amount >= 1) stringResource(R.string.goal_day_done) else stringResource(R.string.action_mark_done),
                            fontFamily = AppUiFontFamily,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StepButton("−", onDecrement, !state.skipped && state.amount > 0)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.amount.toString(), color = if (state.skipped) SalatOutlineVariant else SalatPrimary, fontSize = 68.sp, lineHeight = 72.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
                            Text(
                                stringResource(R.string.goals_v2_of_target, state.target, goalUnitLabel(state.unit)),
                                color = SalatOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontFamily = AppUiFontFamily,
                            )
                        }
                        StepButton("+", onIncrement, !state.skipped && state.amount < state.target)
                    }
                }

                Spacer(Modifier.height(26.dp))
                Button(
                    onClick = onDone,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("goal-log-done"),
                    colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                ) { Text(stringResource(R.string.goals_v2_done), fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium) }

                if (state.canSkip) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.goals_v2_skip_day), color = SalatOnSurface, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f))
                        Switch(state.skipped, onSkippedChanged, colors = salatSwitchColors())
                    }
                    HorizontalDivider(color = SalatOutlineVariant)
                }
                Text(
                    stringResource(R.string.goal_log_clear),
                    color = SalatError,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppUiFontFamily,
                    modifier = Modifier.clickable(onClick = onClear).padding(16.dp),
                )
                Text(
                    stringResource(R.string.goals_v2_auto_save_hint),
                    color = SalatOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                    textAlign = TextAlign.Center,
                )
                if (state.isSaving) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SalatPrimary)
                }
            }
        }
        state.errorRes?.let { errorRes ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
        }
    }
}
