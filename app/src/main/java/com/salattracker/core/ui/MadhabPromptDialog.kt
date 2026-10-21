package com.salattracker.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.salattracker.R
import com.salattracker.core.theme.InterFamily
import com.salattracker.core.theme.MontserratFamily
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatSurfaceContainerLow
import com.salattracker.core.theme.SalatSurfaceContainerLowest
import com.salattracker.domain.model.Madhab

/**
 * One-time prompt shown (from `MainActivity`) to an install that completed onboarding before
 * [Madhab] existed, so it never saw the onboarding school-of-thought step. "Standard" is
 * pre-selected — that's today's behaviour, so a user who just taps Save sees no change — but the
 * prompt still surfaces the choice so Hanafi users notice and can correct Asr.
 *
 * Deliberately not dismissable (no back-press, no tap-outside): it needs exactly one answer so
 * [com.salattracker.domain.repository.UserPrefs.madhabChosen] actually becomes true and this
 * stops appearing.
 */
@Composable
fun MadhabPromptDialog(onSave: (Madhab) -> Unit) {
    var selected by remember { mutableStateOf(Madhab.STANDARD) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = SalatSurfaceContainerLowest,
        title = {
            Text(
                stringResource(R.string.school_prompt_title),
                color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.school_prompt_body), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily)
                Spacer(Modifier.height(2.dp))
                MadhabPromptOption(
                    title = stringResource(R.string.school_standard_title),
                    subtitle = stringResource(R.string.school_standard_desc),
                    isSelected = selected == Madhab.STANDARD,
                    onClick = { selected = Madhab.STANDARD },
                )
                MadhabPromptOption(
                    title = stringResource(R.string.school_hanafi_title),
                    subtitle = stringResource(R.string.school_hanafi_desc),
                    isSelected = selected == Madhab.HANAFI,
                    onClick = { selected = Madhab.HANAFI },
                )
                MadhabPromptOption(
                    title = stringResource(R.string.school_jafari_title),
                    subtitle = stringResource(R.string.school_jafari_desc),
                    isSelected = selected == Madhab.JAFARI,
                    onClick = { selected = Madhab.JAFARI },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) {
                Text(stringResource(R.string.school_prompt_save), color = SalatPrimary, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun MadhabPromptOption(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SalatSecondaryContainer.copy(alpha = 0.4f) else SalatSurfaceContainerLow,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = SalatPrimary))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
                Text(subtitle, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }
        }
    }
}
