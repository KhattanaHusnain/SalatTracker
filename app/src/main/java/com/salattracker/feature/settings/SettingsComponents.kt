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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.CalculationMethod

// ── Calculation-method picker ──────────────────────────────────────────────────
// A dropdown doesn't scale to 23 entries — a bottom sheet with a scrollable list does, and
// matches the checkmark-on-selected convention already used by PrayerStatusBottomSheet.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationMethodSheet(
    selected: CalculationMethod,
    onSelect: (CalculationMethod) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Text(
                stringResource(R.string.settings_calculation_method_sheet_title),
                color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(CalculationMethod.values()) { method ->
                    CalculationMethodRow(
                        method = method,
                        isSelected = method == selected,
                        onClick = { onSelect(method) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculationMethodRow(method: CalculationMethod, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = method.displayName,
            modifier = Modifier.weight(1f),
            color = if (isSelected) SalatPrimary else SalatOnSurface,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = InterFamily,
        )
        if (isSelected) {
            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

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
            Text(label, color = SalatOnSurface, fontSize = 16.sp, fontFamily = InterFamily)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (trailingText != null) {
                Text(trailingText, color = trailingColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
