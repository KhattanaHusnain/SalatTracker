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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.DayDetailBottomSheet
import com.salattracker.core.ui.DayDetailPrayerRow
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

// ── Calendar Screen ───────────────────────────────────────────────────────────

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onDayDetailDismiss: () -> Unit,
    onStatusSave: (PrayerType, PrayerStatus) -> Unit,
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
                text       = uiState.visibleMonth.format(monthYearFormatter),
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

        // ── Verse of the day ──────────────────────────────────────────────────
        uiState.verse?.let { verse ->
            Spacer(modifier = Modifier.height(20.dp))
            VerseCard(verse)
        }
    }

    // ── Day Detail Bottom Sheet (opens when a day cell is tapped) ─────────────
    // DayDetailBottomSheet owns its own nested status picker internally,
    // so onStatusSave here is the final (type, status) to persist directly.
    uiState.selectedDay?.let { detail ->
        DayDetailBottomSheet(
            date       = detail.date,
            hijriLabel = detail.hijriLabel,
            prayers    = detail.prayers.map { p ->
                DayDetailPrayerRow(
                    type       = p.type,
                    timeLabel  = p.time,
                    status     = p.status,
                    isMarkable = p.isMarkable,
                )
            },
            isSaving     = uiState.isSaving,
            onDismiss    = onDayDetailDismiss,
            onStatusSave = onStatusSave,
        )
    }
}

// ── Dot legend ────────────────────────────────────────────────────────────────

private data class LegendItem(val color: Color, val labelRes: Int)
private val legendItems = listOf(
    LegendItem(Color(0xFF10B981), R.string.status_congregation),
    LegendItem(Color(0xFFEAB308), R.string.status_individual),
    LegendItem(Color(0xFFEF4444), R.string.status_qaza),
    LegendItem(Color(0xFFA855F7), R.string.status_qaza_done),
    LegendItem(Color(0xFFCBD5E1), R.string.status_not_recorded),
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
                    Text(stringResource(item.labelRes), color = SalatOnSurfaceVariant, fontSize = 9.sp, fontFamily = InterFamily, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Verse card ────────────────────────────────────────────────────────────────

@Composable
private fun VerseCard(verse: VerseInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatPrimaryContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text       = "“${verse.text}”",
                color      = SalatOnPrimary,
                fontSize   = 14.sp,
                fontFamily = InterFamily,
                lineHeight = 20.sp,
            )
            if (verse.citation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = verse.citation,
                    color      = SalatOnPrimaryContainer,
                    fontSize   = 11.sp,
                    fontFamily = InterFamily,
                )
            }
        }
    }
}

// ── Month grid ────────────────────────────────────────────────────────────────

private val DAY_HEADERS = listOf(
    R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat,
)

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
            DAY_HEADERS.forEach { labelRes ->
                Text(
                    text      = stringResource(labelRes),
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
            verse = VerseInfo(
                text = "Indeed, prayer prohibits immorality and wrongdoing, and the remembrance of Allāh is greater.",
                citation = "Al-Ankabut 29:45",
            ),
        ),
        onPreviousMonth    = {},
        onNextMonth        = {},
        onDaySelected      = {},
        onDayDetailDismiss = {},
        onStatusSave       = { _, _ -> },
    )
}