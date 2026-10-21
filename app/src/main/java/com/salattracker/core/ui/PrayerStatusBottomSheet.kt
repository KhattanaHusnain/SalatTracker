package com.salattracker.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Args passed into the sheet ────────────────────────────────────────────────

data class PrayerStatusSheetArgs(
    val prayerType: PrayerType,
    val date: LocalDate,
    val currentStatus: PrayerStatus,
)

// ── Reusable Bottom Sheet ─────────────────────────────────────────────────────
/**
 * Reusable modal bottom sheet for recording / updating a prayer status.
 * Consumed from HomeScreen, CalendarScreen day-detail, and anywhere else
 * that needs to edit a single prayer entry.
 *
 * Example:
 *   var sheetArgs by remember { mutableStateOf<PrayerStatusSheetArgs?>(null) }
 *
 *   sheetArgs?.let { args ->
 *       PrayerStatusBottomSheet(
 *           args      = args,
 *           isSaving  = uiState.isSaving,
 *           onDismiss = { sheetArgs = null },
 *           onSave    = { status -> viewModel.updateStatus(args, status) },
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerStatusBottomSheet(
    args: PrayerStatusSheetArgs,
    onDismiss: () -> Unit,
    onSave: (PrayerStatus) -> Unit,
    isSaving: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember(args) { mutableStateOf(args.currentStatus) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalatSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SalatSurfaceContainerHighest)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${args.prayerType.label()} \u2013 ${args.date.format(DateTimeFormatter.ofPattern("d MMM"))}",
                    color = SalatPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppTitleFontFamily,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sheet_update_status),
                    color = SalatOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = AppUiFontFamily,
                )
            }

            // Status options
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                prayerStatusItems().forEach { item ->
                    StatusOptionRow(
                        item = item,
                        isSelected = selected == item.status,
                        onClick = { selected = item.status },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { onSave(selected) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SalatPrimary,
                    contentColor = SalatOnPrimary,
                    disabledContainerColor = SalatPrimaryContainer,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SalatOnPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sheet_updating), fontFamily = AppUiFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sheet_save_status), fontFamily = AppUiFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Single option row ─────────────────────────────────────────────────────────

@Composable
private fun StatusOptionRow(
    item: PrayerStatusItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) SalatPrimary else SalatOutlineVariant
    val bgColor     = if (isSelected) SalatSecondaryContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SalatSurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = item.iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(item.status.labelRes),
            modifier = Modifier.weight(1f),
            color = SalatOnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle_filled),
                contentDescription = stringResource(R.string.selected_label),
                tint = SalatPrimary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_radio_unchecked),
                contentDescription = null,
                tint = SalatOutlineVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ── Display model ─────────────────────────────────────────────────────────────

private data class PrayerStatusItem(
    val status: PrayerStatus,
    val iconRes: Int,
    val iconTint: Color,
)

/** Built fresh every call rather than cached in a `val` — `SalatPrimary`/`SalatTertiary`/
 * `SalatError` are theme-aware composable getters now, so this list can't be computed once at
 * class-load time (outside any composition) the way it used to be. */
@Composable
private fun prayerStatusItems(): List<PrayerStatusItem> = listOf(
    PrayerStatusItem(PrayerStatus.CONGREGATION,   R.drawable.ic_groups,          SalatPrimary),
    PrayerStatusItem(PrayerStatus.INDIVIDUAL,     R.drawable.ic_person,          SalatPrimary),
    PrayerStatusItem(PrayerStatus.QAZA_PENDING,   R.drawable.ic_pending_actions, SalatTertiary),
    PrayerStatusItem(PrayerStatus.QAZA_COMPLETED, R.drawable.ic_history_edu,     Color(0xFFFF9939)),
    PrayerStatusItem(PrayerStatus.MENSTRUATION,   R.drawable.ic_female,          SalatError),
)

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun PrayerStatusBottomSheetPreview() {
    PrayerStatusBottomSheet(
        args = PrayerStatusSheetArgs(PrayerType.FAJR, LocalDate.now(), PrayerStatus.CONGREGATION),
        onDismiss = {},
        onSave = {},
    )
}