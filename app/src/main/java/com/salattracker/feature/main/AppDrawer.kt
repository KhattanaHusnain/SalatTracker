package com.salattracker.feature.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSurface
import com.salattracker.core.ui.ArchitecturalFrieze

/** Stateless drawer content: Prayer Reminders, Mosque Mode, Backup & Restore (with an
 * attention line when [backupNeedsAttention]), Stats, Tasbih, Developers, Quran Sources, Settings.
 * Fixed-width so the dimmed scrim area next to it stays tappable (to close the drawer) even on
 * small phones. */
@Composable
fun AppDrawer(
    backupNeedsAttention: Boolean,
    onPrayerRemindersClick: () -> Unit,
    onMosqueModeClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDevelopersClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTasbihClick: () -> Unit,
    onQuranCreditsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = SalatSurface,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                ArchitecturalFrieze(
                    modifier = Modifier.align(Alignment.CenterEnd).size(112.dp, 18.dp),
                    alpha = .18f,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    color = SalatPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppTitleFontFamily,
                )
            }
            HorizontalDivider(color = SalatOutlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            DrawerItem(
                iconRes = R.drawable.ic_notifications,
                label = stringResource(R.string.prayer_reminders),
                onClick = onPrayerRemindersClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_mosque_outline,
                label = stringResource(R.string.mosque_mode),
                onClick = onMosqueModeClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_cloud_upload,
                label = stringResource(R.string.backup_title),
                subtitle = if (backupNeedsAttention) stringResource(R.string.backup_action_needed) else null,
                onClick = onBackupClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_nav_stats,
                label = stringResource(R.string.nav_stats),
                onClick = onStatsClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_auto_awesome,
                label = stringResource(R.string.tasbih_title),
                onClick = onTasbihClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_code,
                label = stringResource(R.string.action_developers),
                onClick = onDevelopersClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_info_circle,
                label = stringResource(R.string.topbar_quran_credits),
                onClick = onQuranCreditsClick,
            )
            DrawerItem(
                iconRes = R.drawable.ic_nav_settings,
                label = stringResource(R.string.nav_settings),
                onClick = onSettingsClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    iconRes: Int,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        selected = false,
        onClick = onClick,
        icon = { Icon(painterResource(iconRes), contentDescription = null, tint = SalatPrimary) },
        label = {
            Column {
                Text(label, fontSize = 14.sp, fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle,  fontSize = 12.sp, fontFamily = AppUiFontFamily, color = SalatError)
                }
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppDrawerPreview() {
    AppDrawer(
        backupNeedsAttention = true,
        onPrayerRemindersClick = {},
        onMosqueModeClick = {},
        onBackupClick = {},
        onDevelopersClick = {},
        onStatsClick = {},
        onTasbihClick = {},
        onQuranCreditsClick = {},
        onSettingsClick = {},
    )
}
