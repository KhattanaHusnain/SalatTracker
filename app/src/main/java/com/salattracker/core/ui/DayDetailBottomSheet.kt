package com.salattracker.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Data model for a single prayer row in a day's detail ──────────────────────
// Shared by Calendar's Month view: DayDetailHeader + DayDetailPrayerRowItem render a day's
// full 5-prayer picture for its inline day panel; there is no modal sheet version of this any
// more — Month and Month-grid merged into one scrollable view with the panel always visible.

data class DayDetailPrayerRow(
    val type: PrayerType,
    val timeLabel: String,          // e.g. "05:42 AM"
    val status: PrayerStatus,
    val isMarkable: Boolean = true,
)

// ── Header ────────────────────────────────────────────────────────────────────
/**
 * [hijriLabel] is optional: pass null when a Hijri conversion isn't available (or not wanted)
 * for this date, and the subtitle falls back to just the completed-count summary.
 */
@Composable
internal fun DayDetailHeader(
    date: LocalDate,
    hijriLabel: String?,
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
            val summary = if (allDone) stringResource(R.string.sheet_all_logged) else stringResource(R.string.sheet_n_of_m_logged, completedCount, totalCount)
            Text(
                text     = if (hijriLabel != null) "$hijriLabel  •  $summary" else summary,
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
                    text       = stringResource(R.string.sheet_complete_count, completedCount, totalCount),
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
internal fun DayDetailPrayerRowItem(
    row: DayDetailPrayerRow,
    onClick: () -> Unit,
) {
    Surface(
        onClick      = onClick,
        enabled      = row.isMarkable,
        shape        = RoundedCornerShape(12.dp),
        color        = SalatSurface,
        border       = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
        modifier     = Modifier.fillMaxWidth().alpha(if (row.isMarkable) 1f else 0.6f),
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
                    text       = row.type.label(),
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
            PrayerStatusChip(status = row.status, isUpcoming = !row.isMarkable)
        }
    }
}

// ── Richer status chip (matches the HTML design tokens exactly) ───────────────

@Composable
internal fun PrayerStatusChip(status: PrayerStatus, isUpcoming: Boolean = false) {
    val (bg, fg, iconRes, label) = if (status == PrayerStatus.NOT_RECORDED && isUpcoming) {
        StatusChipStyle(SalatSurfaceContainerHigh, SalatOnSurfaceVariant, R.drawable.ic_schedule, stringResource(R.string.status_upcoming))
    } else when (status) {
        PrayerStatus.CONGREGATION   -> StatusChipStyle(
            SalatPrimaryFixed, SalatOnPrimaryFixedVariant,
            R.drawable.ic_groups, stringResource(R.string.status_jamaat)
        )
        PrayerStatus.INDIVIDUAL     -> StatusChipStyle(
            SalatSecondaryContainer, SalatOnSecondaryContainer,
            R.drawable.ic_person, stringResource(R.string.status_alone)
        )
        PrayerStatus.QAZA_PENDING   -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            R.drawable.ic_history_edu, stringResource(R.string.status_qaza)
        )
        PrayerStatus.QAZA_COMPLETED -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            R.drawable.ic_check_circle_filled, stringResource(R.string.status_qaza_done)
        )
        PrayerStatus.MENSTRUATION   -> StatusChipStyle(
            SalatErrorContainer, SalatOnErrorContainer,
            R.drawable.ic_female, stringResource(R.string.status_excused)
        )
        PrayerStatus.NOT_RECORDED   -> StatusChipStyle(
            SalatSurfaceContainerHigh, SalatOnSurfaceVariant,
            R.drawable.ic_radio_unchecked, stringResource(R.string.status_not_set)
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

internal val PrayerType.iconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

internal val GoalCategory.iconRes get() = when (this) {
    GoalCategory.SALAT   -> R.drawable.ic_mosque
    GoalCategory.QURAN   -> R.drawable.ic_history_edu
    GoalCategory.FASTING -> R.drawable.ic_schedule
    GoalCategory.CHARITY -> R.drawable.ic_workspace_premium
    GoalCategory.DHIKR   -> R.drawable.ic_local_fire_department
}
