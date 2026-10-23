package com.salattracker.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.CalculationMethod

// ── Navigation row (Prayer Reminders / Backup & Restore) ──────────────────────

@Composable
fun SettingsNavRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    trailingColor: Color = SalatOnSurfaceVariant,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), null, tint = SalatPrimary, modifier = Modifier.size(24.dp))
            Text(label, color = SalatOnSurface, fontSize = 16.sp, fontFamily = AppUiFontFamily)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (trailingText != null) {
                Text(trailingText, color = trailingColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Minute stepper (per-prayer time correction) ────────────────────────────────

/** A "-" / value / "+" control for a small signed-minutes value, clamped to [range]. Used by the
 * "Adjust prayer times" card — one per [com.salattracker.domain.model.PrayerType]. */
@Composable
fun MinuteStepper(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    range: IntRange = -30..30,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = { if (minutes > range.first) onMinutesChanged(minutes - 1) },
            enabled = minutes > range.first,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(painterResource(R.drawable.ic_remove), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
        }
        Text(
            text = stringResource(R.string.settings_prayer_time_adjust_value, minutes),
            modifier = Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.Center,
            color = SalatOnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
        IconButton(
            onClick = { if (minutes < range.last) onMinutesChanged(minutes + 1) },
            enabled = minutes < range.last,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
        }
    }
}
