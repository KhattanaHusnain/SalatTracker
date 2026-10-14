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

/** Views selectable from the header's chip today — grows as Month-grid, Week and Year land. */
private val ENABLED_VIEWS = listOf(CalendarViewType.MONTH)

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
    onPrayerClick: (LocalDate, PrayerType) -> Unit,
    onStatusSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var showGoToDate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
        ) {
            CalendarHeader(
                currentView  = uiState.view,
                onViewChanged = onViewChanged,
                selectedDate = uiState.selectedDate,
                showToday    = uiState.awayFromToday,
                onTodayClick = onTodayClick,
                onPrevious   = onPreviousPeriod,
                onNext       = onNextPeriod,
                onTitleClick = { showGoToDate = true },
            )

            Spacer(modifier = Modifier.height(12.dp))
            DotLegend()
            Spacer(modifier = Modifier.height(16.dp))

            MonthRingGrid(
                dates        = monthGridDates(YearMonth.from(uiState.selectedDate)),
                today        = uiState.today,
                selectedDate = uiState.selectedDate,
                currentMonth = YearMonth.from(uiState.selectedDate),
                isLoading    = uiState.isLoadingMarks,
                marks        = uiState.marks,
                onDaySelected = onDaySelected,
            )

            Spacer(modifier = Modifier.height(20.dp))
            SelectedDayPanel(
                date         = uiState.selectedDate,
                today        = uiState.today,
                statuses     = uiState.selectedDayStatuses,
                times        = uiState.selectedTimes,
                onPrayerClick = { type -> onPrayerClick(uiState.selectedDate, type) },
            )

            uiState.verse?.let { verse ->
                Spacer(modifier = Modifier.height(20.dp))
                VerseCard(verse)
            }
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

    uiState.statusSheet?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onStatusSheetDismiss,
            onSave    = onStatusSave,
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

// ── Month ring grid ───────────────────────────────────────────────────────────

private val DAY_HEADERS = listOf(
    R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat,
)
private const val FRIDAY_COLUMN_INDEX = 5

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
        Box {
            Row(modifier = Modifier.matchParentSize()) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .let { if (index == FRIDAY_COLUMN_INDEX) it.background(SalatSurfaceContainerLow, RoundedCornerShape(10.dp)) else it },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                dates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (isLoading) {
                                DayCellSkeleton(modifier = Modifier.weight(1f).padding(2.dp))
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

@Composable
private fun DayCellSkeleton(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk")
        .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier = modifier.aspectRatio(1f).clip(CircleShape).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
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
    val hijri = remember(date) { date.toHijriOrNull() }
    val hijriText = hijri?.fullLabel()
    val completed = completedCount(statuses.values.toList())
    val summaryText = stringResource(R.string.sheet_n_of_m_logged, completed, PrayerType.values().size)
    val subtitleText = if (hijriText != null) "$hijriText  •  $summaryText" else summaryText
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(date.format(dateFormatter), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
        Text(subtitleText, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        Spacer(modifier = Modifier.height(10.dp))
        PrayerType.values().forEach { type ->
            val status = statuses[type] ?: PrayerStatus.NOT_RECORDED
            val isMarkable = isPrayerMarkable(date, times?.get(type), today)
            CalendarPrayerRow(
                type          = type,
                time          = times?.get(type),
                timeFormatter = timeFormatter,
                status        = status,
                isMarkable    = isMarkable,
                onClick       = { if (isMarkable) onPrayerClick(type) },
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CalendarPrayerRow(
    type: PrayerType,
    time: LocalTime?,
    timeFormatter: DateTimeFormatter,
    status: PrayerStatus,
    isMarkable: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick  = onClick,
        enabled  = isMarkable,
        modifier = Modifier.fillMaxWidth().alpha(if (isMarkable) 1f else 0.55f),
        shape    = RoundedCornerShape(10.dp),
        color    = SalatSurface,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusRingColor(status)))
            Spacer(modifier = Modifier.width(10.dp))
            Text(type.label(), modifier = Modifier.weight(1f), color = SalatOnSurface, fontSize = 14.sp, fontFamily = InterFamily)
            Text(time?.format(timeFormatter) ?: "--:--", color = SalatOutline, fontSize = 11.sp, fontFamily = InterFamily)
            Spacer(modifier = Modifier.width(10.dp))
            Text(status.label(), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFamily)
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
        onPrayerClick        = { _, _ -> },
        onStatusSheetDismiss = {},
        onStatusSave         = {},
        onErrorDismiss       = {},
    )
}
