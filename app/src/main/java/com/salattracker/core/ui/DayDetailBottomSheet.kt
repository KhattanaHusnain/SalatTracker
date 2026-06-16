package com.salattracker.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

// ── Data model for a single prayer row inside the day detail sheet ────────────

data class DayDetailPrayerRow(
    val type: PrayerType,
    val timeLabel: String,          // e.g. "05:42 AM"
    val status: PrayerStatus,
)

// ── Day Detail Bottom Sheet ───────────────────────────────────────────────────
/**
 * Full-height modal bottom sheet showing all 5 prayers for a selected day.
 *
 * Tapping any prayer row opens the nested [PrayerStatusBottomSheet].
 *
 * Usage:
 *   var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
 *
 *   selectedDate?.let { date ->
 *       DayDetailBottomSheet(
 *           date        = date,
 *           hijriLabel  = "22 Rajab 1445",
 *           prayers     = uiState.dayPrayers,
 *           isSaving    = uiState.isSaving,
 *           onDismiss   = { selectedDate = null },
 *           onStatusSave = { type, status -> viewModel.save(date, type, status) },
 *       )
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    date: LocalDate,
    hijriLabel: String,
    prayers: List<DayDetailPrayerRow>,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onStatusSave: (PrayerType, PrayerStatus) -> Unit,
) {
    // Tracks which prayer row is currently open in the nested status sheet
    var sheetTarget by remember { mutableStateOf<PrayerType?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SalatSurfaceBright,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
        val completedCount = prayers.count {
            it.status == PrayerStatus.CONGREGATION || it.status == PrayerStatus.INDIVIDUAL
        }

        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ────────────────────────────────────────────────────────
            DayDetailHeader(
                date           = date,
                hijriLabel     = hijriLabel,
                completedCount = completedCount,
                totalCount     = prayers.size,
                modifier       = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Scrollable prayer rows ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp), // space for footer
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                prayers.forEach { row ->
                    DayDetailPrayerRowItem(
                        row     = row,
                        onClick = { sheetTarget = row.type },
                    )
                }
            }
        }

        // ── Sticky "Done" footer ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f   to Color.Transparent,
                        0.3f to SalatSurfaceBright,
                        1f   to SalatSurfaceBright,
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Button(
                onClick   = onDismiss,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = SalatPrimary,
                    contentColor   = SalatOnPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle_filled),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Done",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
            }
        }
    }

    // ── Nested status sheet (opens on top of this sheet) ─────────────────────
    sheetTarget?.let { targetType ->
        val currentStatus = prayers.find { it.type == targetType }?.status
            ?: PrayerStatus.NOT_RECORDED
        PrayerStatusBottomSheet(
            args = PrayerStatusSheetArgs(
                prayerType    = targetType,
                date          = date,
                currentStatus = currentStatus,
            ),
            isSaving  = isSaving,
            onDismiss = { sheetTarget = null },
            onSave    = { newStatus ->
                onStatusSave(targetType, newStatus)
                sheetTarget = null
            },
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun DayDetailHeader(
    date: LocalDate,
    hijriLabel: String,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMM") }
    val allDone       = completedCount == totalCount

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = date.format(dateFormatter),
                color      = SalatPrimary,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFamily,
                lineHeight = 34.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = "$hijriLabel  •  ${if (allDone) "All prayers logged" else "$completedCount of $totalCount logged"}",
                color    = SalatOnSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = InterFamily,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Completion badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SalatPrimaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (allDone) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle_filled),
                        contentDescription = null,
                        tint    = SalatPrimaryFixed,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text       = "$completedCount/$totalCount Complete",
                    color      = SalatOnPrimaryContainer,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

// ── Single prayer row ─────────────────────────────────────────────────────────

@Composable
private fun DayDetailPrayerRowItem(
    row: DayDetailPrayerRow,
    onClick: () -> Unit,
) {
    val ripple = rememberUpdatedState(onClick)

    Surface(
        onClick      = onClick,
        shape        = RoundedCornerShape(12.dp),
        color        = SalatSurface,
        border       = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
        modifier     = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SalatSecondaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter         = painterResource(row.type.iconRes),
                    contentDescription = null,
                    tint            = SalatOnSecondaryFixedVariant,
                    modifier        = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = row.type.displayName,
                    color      = SalatPrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
                Text(
                    text     = row.timeLabel,
                    color    = SalatOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = InterFamily,
                )
            }

            // Status chip
            PrayerStatusChip(status = row.status)
        }
    }
}

// ── Richer status chip (matches the HTML design tokens exactly) ───────────────

@Composable
private fun PrayerStatusChip(status: PrayerStatus) {
    val (bg, fg, iconRes, label) = when (status) {
        PrayerStatus.CONGREGATION   -> StatusChipStyle(
            SalatPrimaryFixed, SalatOnPrimaryFixedVariant,
            R.drawable.ic_groups, "Jamā'at"
        )
        PrayerStatus.INDIVIDUAL     -> StatusChipStyle(
            SalatSecondaryContainer, SalatOnSecondaryContainer,
            R.drawable.ic_person, "Alone"
        )
        PrayerStatus.QAZA_PENDING   -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            R.drawable.ic_history_edu, "Qaza"
        )
        PrayerStatus.QAZA_COMPLETED -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            R.drawable.ic_check_circle_filled, "Qaza Done"
        )
        PrayerStatus.MENSTRUATION   -> StatusChipStyle(
            SalatErrorContainer, SalatOnErrorContainer,
            R.drawable.ic_female, "Excused"
        )
        PrayerStatus.NOT_RECORDED   -> StatusChipStyle(
            SalatSurfaceContainerHigh, SalatOnSurfaceVariant,
            R.drawable.ic_radio_unchecked, "Not Set"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter         = painterResource(iconRes),
                contentDescription = null,
                tint            = fg,
                modifier        = Modifier.size(14.dp),
            )
            Text(
                text       = label,
                color      = fg,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFamily,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

// Small data holder to keep when() arms concise
private data class StatusChipStyle(
    val bg: Color,
    val fg: Color,
    val iconRes: Int,
    val label: String,
)

// ── Extension helpers ─────────────────────────────────────────────────────────

private val PrayerType.iconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

val PrayerType.displayName get() = when (this) {
    PrayerType.FAJR    -> "Fajr"
    PrayerType.DHUHR   -> "Dhuhr"
    PrayerType.ASR     -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA    -> "Isha"
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DayDetailBottomSheetPreview() {
    DayDetailBottomSheet(
        date       = LocalDate.of(2024, 1, 22),
        hijriLabel = "22 Rajab 1445",
        prayers    = listOf(
            DayDetailPrayerRow(PrayerType.FAJR,    "05:42 AM", PrayerStatus.CONGREGATION),
            DayDetailPrayerRow(PrayerType.DHUHR,   "12:15 PM", PrayerStatus.INDIVIDUAL),
            DayDetailPrayerRow(PrayerType.ASR,     "03:30 PM", PrayerStatus.CONGREGATION),
            DayDetailPrayerRow(PrayerType.MAGHRIB, "05:58 PM", PrayerStatus.QAZA_PENDING),
            DayDetailPrayerRow(PrayerType.ISHA,    "07:15 PM", PrayerStatus.CONGREGATION),
        ),
        onDismiss    = {},
        onStatusSave = { _, _ -> },
    )
}