package com.salattracker.feature.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.salattracker.core.theme.InterFamily
import com.salattracker.core.theme.MontserratFamily
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSurface

/** Stateless drawer content: Bulk Update, Settings. Prayer Reminders and Backup & Restore moved
 * into Settings as navigation rows (see SettingsScreen's "Notifications & Data" card) — Settings
 * is now a hub rather than one of several flat drawer destinations. Fixed-width so the dimmed
 * scrim area next to it stays tappable (to close the drawer) even on small phones. */
@Composable
fun AppDrawer(
    onBulkUpdateClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = SalatSurface,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            color = SalatPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFamily,
        )
        HorizontalDivider(color = SalatOutlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem(
            iconRes = R.drawable.ic_edit_square,
            label = stringResource(R.string.action_bulk_update),
            onClick = onBulkUpdateClick,
        )
        DrawerItem(
            iconRes = R.drawable.ic_nav_settings,
            label = stringResource(R.string.nav_settings),
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun DrawerItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        selected = false,
        onClick = onClick,
        icon = { Icon(painterResource(iconRes), contentDescription = null) },
        label = { Text(label, fontSize = 14.sp, fontFamily = InterFamily, fontWeight = FontWeight.Medium) },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppDrawerPreview() {
    AppDrawer(
        onBulkUpdateClick = {},
        onSettingsClick = {},
    )
}
