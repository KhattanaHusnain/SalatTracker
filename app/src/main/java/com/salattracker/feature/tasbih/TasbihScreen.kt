package com.salattracker.feature.tasbih

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatBackground
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnPrimaryContainer
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatSurfaceContainerLow
import com.salattracker.core.theme.SalatSurfaceContainerLowest
import com.salattracker.domain.model.TasbihPhrase
import com.salattracker.domain.model.TasbihSession

private data class PendingConfiguration(val phrase: TasbihPhrase, val target: Int)

@Composable
fun TasbihScreen(
    uiState: TasbihUiState,
    onIncrement: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onNewRound: () -> Unit,
    onConfigurationChanged: (TasbihPhrase, Int) -> Unit,
    onNoticeDismiss: () -> Unit,
) {
    val session = uiState.session
    val haptics = LocalHapticFeedback.current
    var showPhrases by remember { mutableStateOf(false) }
    var pendingConfiguration by remember { mutableStateOf<PendingConfiguration?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.notice) {
        if (uiState.notice != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(Modifier.fillMaxSize().background(SalatBackground)) {
        if (!uiState.isLoading && session != null) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                uiState.goalName?.let {
                    Surface(shape = RoundedCornerShape(50), color = SalatSecondaryContainer) {
                        Text(stringResource(R.string.tasbih_counting_for, it), modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = SalatOnPrimaryContainer, fontFamily = AppUiFontFamily, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = if (session.phrase == TasbihPhrase.FREE) stringResource(R.string.tasbih_free_count) else session.phrase.arabic,
                    color = SalatPrimary, fontFamily = AppTitleFontFamily, fontSize = if (session.phrase == TasbihPhrase.FREE) 20.sp else 28.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(phraseLabel(session.phrase), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))

                Surface(
                    onClick = {
                        if (!session.isCompleted) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIncrement()
                        }
                    },
                    enabled = !session.isCompleted,
                    modifier = Modifier.size(250.dp),
                    shape = CircleShape,
                    color = SalatSurfaceContainerLowest,
                    border = BorderStroke(2.dp, SalatSecondaryContainer),
                    shadowElevation = 3.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (session.target > 0) {
                            CircularProgressIndicator(
                                progress = { 1f }, modifier = Modifier.size(224.dp), color = SalatSecondaryContainer, strokeWidth = 10.dp,
                            )
                            CircularProgressIndicator(
                                progress = { session.count.toFloat() / session.target }, modifier = Modifier.size(224.dp), color = SalatPrimary, strokeWidth = 10.dp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(session.count.toString(), color = SalatOnSurface, fontSize = 62.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
                            Text(
                                text = if (session.target == 0) stringResource(R.string.tasbih_open_target) else stringResource(R.string.tasbih_of_target, session.target),
                                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily,
                            )
                            if (!session.isCompleted) {
                                Spacer(Modifier.height(10.dp))
                                Text(stringResource(R.string.tasbih_tap_to_count), color = SalatPrimary, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))

                if (session.isCompleted) {
                    Surface(color = SalatPrimaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnPrimaryContainer)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.tasbih_round_complete), color = SalatOnPrimaryContainer, fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onNewRound, colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary)) {
                                Text(stringResource(R.string.tasbih_new_round), fontFamily = AppUiFontFamily)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onUndo, enabled = session.count > 0) {
                            Icon(painterResource(R.drawable.ic_remove), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.tasbih_undo), fontFamily = AppUiFontFamily)
                        }
                        OutlinedButton(onClick = { if (session.count > 0) showResetConfirmation = true else onReset() }) {
                            Icon(painterResource(R.drawable.ic_history), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.tasbih_reset), fontFamily = AppUiFontFamily)
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Surface(color = SalatSurfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.tasbih_phrase), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                        Spacer(Modifier.height(8.dp))
                        Surface(onClick = { showPhrases = true }, color = SalatSurfaceContainerLowest, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, SalatOutlineVariant), modifier = Modifier.fillMaxWidth()) {
                            Text(phraseLabel(session.phrase), modifier = Modifier.padding(14.dp), color = SalatOnSurface, fontFamily = AppUiFontFamily)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tasbih_target), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (if (session.goalId == null) listOf(0, 33, 100) else listOf(33, 100)).forEach { target ->
                                FilterChip(
                                    selected = session.target == target,
                                    onClick = {
                                        if (target != session.target) {
                                            val change = PendingConfiguration(session.phrase, target)
                                            if (session.count > 0 || session.isCompleted) pendingConfiguration = change else onConfigurationChanged(change.phrase, change.target)
                                        }
                                    },
                                    label = { Text(if (target == 0) stringResource(R.string.tasbih_open_target) else target.toString(), fontFamily = AppUiFontFamily) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SalatSecondaryContainer),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showPhrases && session != null) {
        AlertDialog(
            onDismissRequest = { showPhrases = false },
            title = { Text(stringResource(R.string.tasbih_choose_phrase), fontFamily = AppTitleFontFamily) },
            text = {
                Column {
                    TasbihPhrase.entries.forEach { phrase ->
                        Text(
                            text = phraseLabel(phrase),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showPhrases = false
                                val change = PendingConfiguration(phrase, session.target)
                                if (phrase != session.phrase) {
                                    if (session.count > 0 || session.isCompleted) pendingConfiguration = change else onConfigurationChanged(change.phrase, change.target)
                                }
                            }.padding(vertical = 12.dp),
                            color = SalatOnSurface, fontFamily = AppUiFontFamily,
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = { showPhrases = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (pendingConfiguration != null) {
        AlertDialog(
            onDismissRequest = { pendingConfiguration = null },
            title = { Text(stringResource(R.string.tasbih_change_title), fontFamily = AppTitleFontFamily) },
            text = { Text(stringResource(R.string.tasbih_change_desc), fontFamily = AppUiFontFamily) },
            confirmButton = { Button(onClick = { pendingConfiguration?.let { onConfigurationChanged(it.phrase, it.target) }; pendingConfiguration = null }) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { OutlinedButton(onClick = { pendingConfiguration = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.tasbih_reset_title), fontFamily = AppTitleFontFamily) },
            text = { Text(stringResource(R.string.tasbih_reset_desc), fontFamily = AppUiFontFamily) },
            confirmButton = { Button(onClick = { onReset(); showResetConfirmation = false }) { Text(stringResource(R.string.tasbih_reset)) } },
            dismissButton = { OutlinedButton(onClick = { showResetConfirmation = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    uiState.notice?.let { notice ->
        val message = when (notice) {
            TasbihNotice.FreeCompleted -> stringResource(R.string.tasbih_completed)
            TasbihNotice.GoalCompleted -> stringResource(R.string.tasbih_goal_credited)
            TasbihNotice.GoalNotCredited -> stringResource(R.string.tasbih_goal_not_credited)
        }
        AlertDialog(
            onDismissRequest = onNoticeDismiss,
            title = { Text(stringResource(R.string.tasbih_round_complete), fontFamily = AppTitleFontFamily) },
            text = { Text(message, fontFamily = AppUiFontFamily) },
            confirmButton = { Button(onClick = onNoticeDismiss) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun phraseLabel(phrase: TasbihPhrase): String = stringResource(
    when (phrase) {
        TasbihPhrase.SUBHANALLAH -> R.string.tasbih_phrase_subhanallah
        TasbihPhrase.ALHAMDULILLAH -> R.string.tasbih_phrase_alhamdulillah
        TasbihPhrase.ALLAHU_AKBAR -> R.string.tasbih_phrase_allahu_akbar
        TasbihPhrase.LA_ILAHA_ILLA_ALLAH -> R.string.tasbih_phrase_tahlil
        TasbihPhrase.ASTAGHFIRULLAH -> R.string.tasbih_phrase_astaghfirullah
        TasbihPhrase.FREE -> R.string.tasbih_free_count
    }
)
