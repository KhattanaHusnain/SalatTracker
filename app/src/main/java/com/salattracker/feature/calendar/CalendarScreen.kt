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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.usecase.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Views selectable from the header's chip today — grows as Week and Year land. */
private val ENABLED_VIEWS = listOf(CalendarViewType.MONTH, CalendarViewType.MONTH_GRID)

// ── Calendar Screen ───────────────────────────────────────────────────────────

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onViewChanged: (CalendarViewType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onTodayClick: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onGridDayClick: (LocalDate) -> Unit,
    onDaySheetDismiss: () -> Unit,
    onPrayerClick: (LocalDate, PrayerType) -> Unit,
    onStatusSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var showGoToDate by remember { mutableStateOf(false) }
    val visibleMonth = YearMonth.from(uiState.selectedDate)

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.view) {
            CalendarViewType.MONTH -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp),
            ) {
                CalendarHeader(
                    currentView   = uiState.view,
                    onViewChanged = onViewChanged,
                    selectedDate  = uiState.selectedDate,
                    showToday     = uiState.awayFromToday,
                    onTodayClick  = onTodayClick,
                    onPrevious    = onPreviousPeriod,
                    onNext        = onNextPeriod,
                    onTitleClick  = { showGoToDate = true },
                )

                Spacer(modifier = Modifier.height(12.dp))
                DotLegend()
                Spacer(modifier = Modifier.height(16.dp))

                MonthRingGrid(
                    dates         = monthGridDates(visibleMonth),
                    today         = uiState.today,
                    selectedDate  = uiState.selectedDate,
                    currentMonth  = visibleMonth,
                    isLoading     = uiState.isLoadingMarks,
                    marks         = uiState.marks,
                    onDaySelected = onDaySelected,
                )

                Spacer(modifier = Modifier.height(20.dp))
                SelectedDayPanel(
                    date          = uiState.selectedDate,
                    today         = uiState.today,
                    statuses      = uiState.selectedDayStatuses,
                    times         = uiState.selectedTimes,
                    onPrayerClick = { type -> onPrayerClick(uiState.selectedDate, type) },
                )

                uiState.verse?.let { verse ->
                    Spacer(modifier = Modifier.height(20.dp))
                    VerseCard(verse)
                }
            }

            CalendarViewType.MONTH_GRID -> Column(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CalendarHeader(
                        currentView   = uiState.view,
                        onViewChanged = onViewChanged,
                        selectedDate  = uiState.selectedDate,
                        showToday     = uiState.awayFromToday,
                        onTodayClick  = onTodayClick,
                        onPrevious    = onPreviousPeriod,
                        onNext        = onNextPeriod,
                        onTitleClick  = { showGoToDate = true },
                    )
                }
                MonthGridView(
                    dates        = monthGridDates(visibleMonth),
                    today        = uiState.today,
                    currentMonth = visibleMonth,
                    isLoading    = uiState.isLoadingMarks,
                    marks        = uiState.marks,
                    onDayClick   = onGridDayClick,
                )
            }

            CalendarViewType.WEEK, CalendarViewType.YEAR -> Unit // not built yet; unreachable — ENABLED_VIEWS excludes them
        }

        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = InterFamily) }
        }
    }

    if (showGoToDate) {
        DatePickerModal(
            initialDate            = uiState.selectedDate,
            onDatePicked           = { picked -> onDatePicked(picked); showGoToDate = false },
            onDismiss              = { showGoToDate = false },
            earliestSelectableDate = CALENDAR_MIN_DATE,
            latestSelectableDate   = CALENDAR_MAX_DATE,
        )
    }

    if (uiState.daySheetOpen) {
        val hijriText = remember(uiState.selectedDate) { uiState.selectedDate.toHijriOrNull() }?.fullLabel()
        DayDetailBottomSheet(
            date          = uiState.selectedDate,
            hijriLabel    = hijriText,
            prayers       = buildDayDetailRows(uiState.selectedDate, uiState.today, uiState.selectedDayStatuses, uiState.selectedTimes),
            onDismiss     = onDaySheetDismiss,
            onPrayerClick = { type -> onPrayerClick(uiState.selectedDate, type) },
        )
    }

    uiState.statusSheet?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onStatusSheetDismiss,
            onSave    = onStatusSave,
        )
    }
}

// ── Shared day-row building ───────────────────────────────────────────────────

/** Builds the 5-row list [DayDetailBottomSheet] and [SelectedDayPanel] both render, resolving
 * each prayer's time label and markability the same way. */
@Composable
private fun buildDayDetailRows(
    date: LocalDate,
    today: LocalDate,
    statuses: Map<PrayerType, PrayerStatus>,
    times: Map<PrayerType, LocalTime>?,
): List<DayDetailPrayerRow> {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    return PrayerType.values().map { type ->
        val time = times?.get(type)
        DayDetailPrayerRow(
            type       = type,
            timeLabel  = time?.format(timeFormatter) ?: "--:--",
            status     = statuses[type] ?: PrayerStatus.NOT_RECORDED,
            isMarkable = isPrayerMarkable(date, time, today),
        )
    }
}

// ── Header: view chip, Today shortcut, chevrons + title/subtitle ──────────────

@Composable
private fun CalendarHeader(
    currentView: CalendarViewType,
    onViewChanged: (CalendarViewType) -> Unit,
    selectedDate: LocalDate,
    showToday: Boolean,
    onTodayClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val visibleMonth = YearMonth.from(selectedDate)
    val titleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val startHijri = remember(visibleMonth) { visibleMonth.atDay(1).toHijriOrNull() }
    val endHijri = remember(visibleMonth) { visibleMonth.atEndOfMonth().toHijriOrNull() }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            ViewChip(
                currentView   = currentView,
                expanded      = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onViewChanged = onViewChanged,
            )
            if (showToday) {
                TextButton(onClick = onTodayClick) {
                    Icon(painterResource(R.drawable.ic_calendar_today), null, tint = SalatPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.calendar_today), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                }
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.bulk_previous), tint = SalatOnSurfaceVariant)
            }
            Column(
                modifier            = Modifier.weight(1f).clickable(onClick = onTitleClick),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text       = visibleMonth.format(titleFormatter),
                    color      = SalatPrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                    textAlign  = TextAlign.Center,
                )
                if (startHijri != null && endHijri != null) {
                    Text(
                        text       = hijriMonthRangeLabel(startHijri, endHijri),
                        color      = SalatOnSurfaceVariant,
                        fontSize   = 11.sp,
                        fontFamily = InterFamily,
                        textAlign  = TextAlign.Center,
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.bulk_next), tint = SalatOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ViewChip(
    currentView: CalendarViewType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onViewChanged: (CalendarViewType) -> Unit,
) {
    val canSwitch = ENABLED_VIEWS.size > 1
    Box {
        Surface(
            onClick = { if (canSwitch) onExpandedChange(true) },
            shape   = RoundedCornerShape(50),
            color   = SalatSurfaceContainerLow,
            border  = BorderStroke(1.dp, SalatOutlineVariant),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(currentView.label(), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                if (canSwitch) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_expand_more),
                        contentDescription = stringResource(R.string.cd_change_view),
                        tint               = SalatOnSurfaceVariant,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            ENABLED_VIEWS.forEach { view ->
                DropdownMenuItem(
                    text    = { Text(view.label()) },
                    onClick = { onViewChanged(view); onExpandedChange(false) },
                )
            }
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

private data class LegendItem(val color: Color, val labelRes: Int)
private val legendItems = listOf(
    LegendItem(SalatStatusCongregation,  R.string.status_congregation),
    LegendItem(SalatStatusIndividual,    R.string.status_individual),
    LegendItem(SalatStatusQazaPending,   R.string.status_qaza),
    LegendItem(SalatStatusQazaCompleted, R.string.status_qaza_done),
    LegendItem(SalatStatusExcused,       R.string.status_excused),
    LegendItem(SalatStatusNotRecorded,   R.string.status_not_recorded),
)

@Composable
private fun DotLegend() {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(12.dp),
        color          = SalatSurfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            legendItems.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { item -> LegendDot(item) }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(item: LegendItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.color))
        Text(stringResource(item.labelRes), color = SalatOnSurfaceVariant, fontSize = 9.sp, fontFamily = InterFamily, fontWeight = FontWeight.Medium)
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

// ── Shared grid pieces (Month and Month-grid) ─────────────────────────────────

private val DAY_HEADERS = listOf(
    R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat,
)
private const val FRIDAY_COLUMN_INDEX = 5

@Composable
private fun WeekdayHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DAY_HEADERS.forEachIndexed { index, labelRes ->
            val isFriday = index == FRIDAY_COLUMN_INDEX
            Text(
                text          = stringResource(labelRes),
                modifier      = Modifier.weight(1f),
                textAlign     = TextAlign.Center,
                color         = if (isFriday) SalatPrimary else SalatOutline,
                fontSize      = 10.sp,
                fontWeight    = if (isFriday) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily    = InterFamily,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun FridayBand(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(7) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { if (index == FRIDAY_COLUMN_INDEX) it.background(SalatSurfaceContainerLow, RoundedCornerShape(10.dp)) else it },
            )
        }
    }
}

// ── Month ring grid (compact, inside the scrolling Month view) ───────────────

@Composable
private fun MonthRingGrid(
    dates: List<LocalDate>,
    today: LocalDate,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    isLoading: Boolean,
    marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onDaySelected: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WeekdayHeaderRow()
        Box {
            FridayBand(modifier = Modifier.matchParentSize())
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                dates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (isLoading) {
                                DayCellSkeleton(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                            } else {
                                RingDayCell(
                                    date           = date,
                                    isToday        = date == today,
                                    isSelected     = date == selectedDate,
                                    isCurrentMonth = YearMonth.from(date) == currentMonth,
                                    statuses       = marks[date].orEmpty(),
                                    showRing       = !date.isAfter(today),
                                    onClick        = { onDaySelected(date) },
                                    modifier       = Modifier.weight(1f).padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Month grid (fills the screen, with Hijri day numbers) ────────────────────

@Composable
private fun MonthGridView(
    dates: List<LocalDate>,
    today: LocalDate,
    currentMonth: YearMonth,
    isLoading: Boolean,
    marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp)) {
        WeekdayHeaderRow()
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FridayBand(modifier = Modifier.matchParentSize())
            Column(modifier = Modifier.fillMaxSize()) {
                dates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        week.forEach { date ->
                            if (isLoading) {
                                DayCellSkeleton(modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp))
                            } else {
                                MonthGridDayCell(
                                    date           = date,
                                    isToday        = date == today,
                                    isCurrentMonth = YearMonth.from(date) == currentMonth,
                                    statuses       = marks[date].orEmpty(),
                                    showRing       = !date.isAfter(today),
                                    onClick        = { onDayClick(date) },
                                    modifier       = Modifier.weight(1f).fillMaxHeight().padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridDayCell(
    date: LocalDate,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    statuses: Map<PrayerType, PrayerStatus>,
    showRing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimAlpha = if (isCurrentMonth) 1f else 0.35f
    val hijriDay = remember(date) { date.toHijriOrNull()?.day }

    Column(
        modifier            = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            if (showRing) {
                Canvas(modifier = Modifier.fillMaxSize().alpha(dimAlpha)) {
                    val strokeWidth = size.minDimension * 0.12f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    val segmentSweep = 360f / PrayerType.values().size
                    val gap = 4f
                    PrayerType.values().forEachIndexed { index, type ->
                        drawArc(
                            color      = statusRingColor(statuses[type]),
                            startAngle = -90f + index * segmentSweep + gap / 2f,
                            sweepAngle = segmentSweep - gap,
                            useCenter  = false,
                            topLeft    = topLeft,
                            size       = arcSize,
                            style      = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            if (isToday) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(SalatPrimary))
            }
            Text(
                text       = date.dayOfMonth.toString(),
                color      = if (isToday) SalatOnPrimary else SalatOnSurface,
                fontSize   = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontFamily = InterFamily,
                modifier   = Modifier.alpha(if (isToday) 1f else dimAlpha),
            )
        }
        if (hijriDay != null) {
            Text(
                text       = hijriDay.toString(),
                color      = if (hijriDay == 1) SalatPrimary else SalatOutline,
                fontSize   = 9.sp,
                fontWeight = if (hijriDay == 1) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = InterFamily,
                modifier   = Modifier.alpha(dimAlpha),
            )
        }
    }
}

@Composable
private fun RingDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    statuses: Map<PrayerType, PrayerStatus>,
    showRing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimAlpha = if (isCurrentMonth) 1f else 0.35f
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .let { if (isSelected) it.background(SalatPrimaryFixed.copy(alpha = 0.18f)) else it }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showRing) {
            Canvas(modifier = Modifier.fillMaxSize(0.82f).alpha(dimAlpha)) {
                val strokeWidth = size.minDimension * 0.09f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                val segmentSweep = 360f / PrayerType.values().size
                val gap = 3f
                PrayerType.values().forEachIndexed { index, type ->
                    drawArc(
                        color      = statusRingColor(statuses[type]),
                        startAngle = -90f + index * segmentSweep + gap / 2f,
                        sweepAngle = segmentSweep - gap,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
        }
        if (isToday) {
            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(SalatPrimary))
        }
        Text(
            text       = date.dayOfMonth.toString(),
            color      = if (isToday) SalatOnPrimary else SalatOnSurface,
            fontSize   = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            fontFamily = InterFamily,
            modifier   = Modifier.alpha(if (isToday) 1f else dimAlpha),
        )
    }
}

/** [modifier] supplies its own sizing — a square aspect ratio for the compact Month grid, a
 * fill-height cell for the full-screen Month-grid view. */
@Composable
private fun DayCellSkeleton(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk")
        .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
}

private fun statusRingColor(status: PrayerStatus?): Color = when (status) {
    PrayerStatus.CONGREGATION   -> SalatStatusCongregation
    PrayerStatus.INDIVIDUAL     -> SalatStatusIndividual
    PrayerStatus.QAZA_PENDING   -> SalatStatusQazaPending
    PrayerStatus.QAZA_COMPLETED -> SalatStatusQazaCompleted
    PrayerStatus.MENSTRUATION   -> SalatStatusExcused
    PrayerStatus.NOT_RECORDED, null -> SalatStatusNotRecorded
}

// ── Selected day panel ─────────────────────────────────────────────────────────

@Composable
private fun SelectedDayPanel(
    date: LocalDate,
    today: LocalDate,
    statuses: Map<PrayerType, PrayerStatus>,
    times: Map<PrayerType, LocalTime>?,
    onPrayerClick: (PrayerType) -> Unit,
) {
    val hijriText = remember(date) { date.toHijriOrNull() }?.fullLabel()
    val rows = buildDayDetailRows(date, today, statuses, times)

    Column(modifier = Modifier.fillMaxWidth()) {
        DayDetailHeader(
            date           = date,
            hijriLabel     = hijriText,
            completedCount = completedCount(rows.map { it.status }),
            totalCount     = rows.size,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                DayDetailPrayerRowItem(row = row, onClick = { if (row.isMarkable) onPrayerClick(row.type) })
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    val today = LocalDate.now()
    CalendarScreen(
        uiState = CalendarUiState(
            view           = CalendarViewType.MONTH,
            today          = today,
            selectedDate   = today,
            isLoadingMarks = false,
            marks = mapOf(
                today to mapOf(
                    PrayerType.FAJR to PrayerStatus.CONGREGATION,
                    PrayerType.DHUHR to PrayerStatus.INDIVIDUAL,
                    PrayerType.ASR to PrayerStatus.CONGREGATION,
                    PrayerType.MAGHRIB to PrayerStatus.QAZA_PENDING,
                ),
            ),
            selectedTimes = null,
            verse = VerseInfo(
                text     = "Indeed, prayer prohibits immorality and wrongdoing, and the remembrance of Allāh is greater.",
                citation = "Al-Ankabut 29:45",
            ),
        ),
        onViewChanged        = {},
        onPreviousPeriod     = {},
        onNextPeriod         = {},
        onTodayClick         = {},
        onDatePicked         = {},
        onDaySelected        = {},
        onGridDayClick       = {},
        onDaySheetDismiss    = {},
        onPrayerClick        = { _, _ -> },
        onStatusSheetDismiss = {},
        onStatusSave         = {},
        onErrorDismiss       = {},
    )
}
