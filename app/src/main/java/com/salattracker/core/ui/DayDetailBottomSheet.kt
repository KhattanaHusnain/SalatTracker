package com.salattracker.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val isActive: Boolean = false,
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
                fontFamily = AppTitleFontFamily,
                lineHeight = 34.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val summary = if (allDone) stringResource(R.string.sheet_all_logged) else stringResource(R.string.sheet_n_of_m_logged, completedCount, totalCount)
            Text(
                text     = if (hijriLabel != null) "$hijriLabel  •  $summary" else summary,
                color    = SalatOnSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = AppUiFontFamily,
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
                       fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppUiFontFamily,
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
    CompactPrayerRow(
        prayerType = row.type,
        enabled = row.isMarkable,
        isActive = row.isActive,
        accentColor = SalatPrimary,
        dividerColor = SalatOutlineVariant,
        iconTint = SalatOnSurfaceVariant,
        modifier = Modifier.clickable(enabled = row.isMarkable, onClick = onClick),
        content = {
                Text(
                    text       = row.type.label(),
                    color      = SalatOnSurface,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AppUiFontFamily,
                )
                Text(
                    text     = row.timeLabel,
                    color    = SalatOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
        },
        actions = {
            PrayerStatusChip(status = row.status, isUpcoming = !row.isMarkable)
        },
    )
}

// ── Richer status chip (matches the HTML design tokens exactly) ───────────────

@Composable
internal fun PrayerStatusChip(status: PrayerStatus, isUpcoming: Boolean = false) {
    val (bg, fg, iconRes, label) = if (status == PrayerStatus.NOT_RECORDED && isUpcoming) {
        StatusChipStyle(SalatSurfaceContainerHigh, SalatOnSurfaceVariant, R.drawable.ic_schedule, stringResource(R.string.status_upcoming))
    } else when (status) {
        PrayerStatus.CONGREGATION   -> StatusChipStyle(
            SalatPrimaryFixed, SalatOnPrimaryFixedVariant,
            PrayerStatus.CONGREGATION.iconRes, stringResource(PrayerStatus.CONGREGATION.labelRes)
        )
        PrayerStatus.INDIVIDUAL     -> StatusChipStyle(
            SalatSecondaryContainer, SalatOnSecondaryContainer,
            PrayerStatus.INDIVIDUAL.iconRes, stringResource(PrayerStatus.INDIVIDUAL.labelRes)
        )
        PrayerStatus.QAZA_PENDING   -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            PrayerStatus.QAZA_PENDING.iconRes, stringResource(PrayerStatus.QAZA_PENDING.labelRes)
        )
        PrayerStatus.QAZA_COMPLETED -> StatusChipStyle(
            Color(0xFFFFDCC3), Color(0xFF6E3900),
            PrayerStatus.QAZA_COMPLETED.iconRes, stringResource(PrayerStatus.QAZA_COMPLETED.labelRes)
        )
        PrayerStatus.MENSTRUATION   -> StatusChipStyle(
            SalatErrorContainer, SalatOnErrorContainer,
            PrayerStatus.MENSTRUATION.iconRes, stringResource(PrayerStatus.MENSTRUATION.labelRes)
        )
        PrayerStatus.NOT_RECORDED   -> StatusChipStyle(
            SalatSurfaceContainerHigh, SalatOnSurfaceVariant,
            PrayerStatus.NOT_RECORDED.iconRes, stringResource(PrayerStatus.NOT_RECORDED.labelRes)
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
                   fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppUiFontFamily,
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
    PrayerType.FAJR    -> R.drawable.home_prayer_fajr
    PrayerType.DHUHR   -> R.drawable.home_prayer_dhuhr
    PrayerType.ASR     -> R.drawable.home_prayer_asr
    PrayerType.MAGHRIB -> R.drawable.home_prayer_maghrib
    PrayerType.ISHA    -> R.drawable.home_prayer_isha
}

internal val GoalCategory.iconRes get() = when (this) {
    GoalCategory.SALAT   -> R.drawable.ic_praying
    GoalCategory.QURAN   -> R.drawable.ic_history_edu
    GoalCategory.FASTING -> R.drawable.ic_schedule
    GoalCategory.CHARITY -> R.drawable.ic_workspace_premium
    GoalCategory.DHIKR   -> R.drawable.ic_local_fire_department
}
