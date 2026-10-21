package com.salattracker.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOutline
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.theme.SalatSurfaceContainerLowest

/** The rounded, bordered card wrapper shared by every settings-shaped screen (Settings,
 * Backup & Restore, Prayer Reminders) — previously duplicated verbatim between Settings and
 * Backup since the original lived as a private composable in SettingsScreen.kt. */
@Composable
fun SettingsCard(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = SalatSurfaceContainerLowest,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Small uppercase label above a group of cards. */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier.fillMaxWidth(),
        color         = SalatPrimaryContainer,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        fontFamily    = AppUiFontFamily,
        letterSpacing = 1.5.sp,
    )
}

/** The app's themed switch colours. */
@Composable
fun salatSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor   = SalatOnPrimary,
    checkedTrackColor   = SalatPrimary,
    uncheckedThumbColor = SalatOutline,
    uncheckedTrackColor = SalatOutlineVariant,
)
