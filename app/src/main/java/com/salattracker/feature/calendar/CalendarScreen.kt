package com.salattracker.feature.calendar

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.DayDetailBottomSheet
import com.salattracker.core.ui.DayDetailPrayerRow
import com.salattracker.core.ui.PrayerStatusBottomSheet
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.YearMonth

// ── Calendar Screen ───────────────────────────────────────────────────────────

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onPrayerRowClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onDayDetailDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 100.dp),
    ) {
        // ── Section header + month nav ────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Spiritual Progress",
                color      = SalatOnSurface,
                fontSize   = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
            )
            Row {
                IconButton(onClick = onPreviousMonth) {
                    Icon(painterResource(R.drawable.ic_chevron_left), null, tint = SalatOnSurfaceVariant)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant)
                }
            }
        }

        // ── Legend ────────────────────────────────────────────────────────────
        DotLegend()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Calendar grid ─────────────────────────────────────────────────────
        MonthGrid(
            month         = uiState.visibleMonth,
            days          = uiState.days,
            isLoading     = uiState.isLoadingMonth,
            onDaySelected = onDaySelected,
        )
    }

    // ── Day Detail Bottom Sheet (opens when a day cell is tapped) ─────────────
    uiState.selectedDay?.let { detail ->
        DayDetailBottomSheet(
            date       = detail.date,
            hijriLabel = detail.hijriLabel,
            prayers    = detail.prayers.map { p ->
                DayDetailPrayerRow(
                    type      = p.type,
                    timeLabel = p.time,
                    status    = p.status,
                )
            },
            isSaving     = uiState.isSaving,
            onDismiss    = onDayDetailDismiss,
            onStatusSave = { type, status ->
                // Update sheetArgs so PrayerStatusBottomSheet opens from inside DayDetail
                onPrayerRowClick(type)
            },
        )
    }

    // ── Standalone status sheet (triggered from within DayDetail) ─────────────
    uiState.sheetArgs?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onSheetDismiss,
            onSave    = onStatusSave,
        )
    }
}

// ── Dot legend ────────────────────────────────────────────────────────────────

private data class LegendItem(val color: Color, val label: String)
private val legendItems = listOf(
    LegendItem(Color(0xFF10B981), "Congregation"),
    LegendItem(Color(0xFFEAB308), "Individual"),
    LegendItem(Color(0xFFEF4444), "Qaza"),
    LegendItem(Color(0xFFA855F7), "Qaza Done"),
    LegendItem(Color(0xFFCBD5E1), "Not Recorded"),
)

@Composable
private fun DotLegend() {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(12.dp),
        color          = SalatSurfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            legendItems.forEach { item ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.color))
                    Text(item.label, color = SalatOnSurfaceVariant, fontSize = 9.sp, fontFamily = InterFamily, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Month grid ────────────────────────────────────────────────────────────────

private val DAY_HEADERS = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

@Composable
private fun MonthGrid(
    month: YearMonth,
    days: List<DayCell>,
    isLoading: Boolean,
    onDaySelected: (LocalDate) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value % 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_HEADERS.forEach { label ->
                Text(
                    text      = label,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color     = SalatOutline,
                    fontSize  = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                    letterSpacing = 1.sp,
                )
            }
        }
        val rowCount = ((offset + days.size) + 6) / 7
        repeat(rowCount) { rowIdx ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { colIdx ->
                    val dayIdx = rowIdx * 7 + colIdx - offset
                    when {
                        dayIdx < 0 || dayIdx >= days.size ->
                            Spacer(modifier = Modifier.weight(1f).height(72.dp))
                        isLoading ->
                            DayCellSkeleton(Modifier.weight(1f))
                        else ->
                            DayCellComposable(
                                cell    = days[dayIdx],
                                onClick = { onDaySelected(days[dayIdx].date) },
                                modifier = Modifier.weight(1f),
                            )
                    }
                }
            }
        }
    }
}

// ── Day cell ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCellComposable(cell: DayCell, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = when {
        cell.isSelected -> SalatSurfaceTint
        cell.isToday    -> SalatPrimary.copy(alpha = 0.4f)
        else            -> SalatSecondaryContainer
    }
    val bgColor = if (cell.isSelected) SalatPrimaryFixed.copy(alpha = 0.12f) else SalatSurface

    Surface(
        onClick  = onClick,
        modifier = modifier.height(72.dp),
        shape    = RoundedCornerShape(8.dp),
        color    = bgColor,
        border   = BorderStroke(if (cell.isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier              = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = cell.date.dayOfMonth.toString(),
                color      = if (cell.isToday) SalatPrimary else SalatOnSurface,
                fontSize   = 13.sp,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                fontFamily = InterFamily,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                cell.dots.forEach { dot ->
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dot.toColor()))
                }
            }
        }
    }
}

@Composable
private fun DayCellSkeleton(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk")
        .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier = modifier.height(72.dp).clip(RoundedCornerShape(8.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
}

private fun DotColor.toColor() = when (this) {
    DotColor.GREEN  -> Color(0xFF10B981)
    DotColor.YELLOW -> Color(0xFFEAB308)
    DotColor.RED    -> Color(0xFFEF4444)
    DotColor.PURPLE -> Color(0xFFA855F7)
    DotColor.GREY   -> Color(0xFFCBD5E1)
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    CalendarScreen(
        uiState = CalendarUiState(
            visibleMonth   = month,
            isLoadingMonth = false,
            days = (1..month.lengthOfMonth()).map { d ->
                DayCell(
                    date          = month.atDay(d),
                    dots          = listOf(DotColor.GREEN, DotColor.GREEN, DotColor.YELLOW, DotColor.GREEN, DotColor.GREY),
                    completionPct = 80,
                    isToday       = month.atDay(d) == today,
                    isSelected    = month.atDay(d) == today,
                )
            },
            selectedDay = null,
        ),
        onPreviousMonth    = {},
        onNextMonth        = {},
        onDaySelected      = {},
        onPrayerRowClick   = {},
        onSheetDismiss     = {},
        onDayDetailDismiss = {},
        onStatusSave       = {},
    )
}