package com.salattracker.feature.calendar

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
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
import com.salattracker.core.ui.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.usecase.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ── Calendar Screen ───────────────────────────────────────────────────────────
// The view switcher and Today shortcut live in the app's shared top bar now (see
// MainActivity's TopBar), driven directly off CalendarViewModel rather than through this
// screen — it only reads uiState.view to decide what to render.

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onMonthTileClick: (YearMonth) -> Unit,
    onBackFromYearDrillDown: () -> Unit,
    onPrayerClick: (LocalDate, PrayerType) -> Unit,
    onQuickStatusSave: (LocalDate, PrayerType, PrayerStatus) -> Unit,
    onStatusSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var showGoToDate by remember { mutableStateOf(false) }
    val visibleMonth = YearMonth.from(uiState.selectedDate)

    // Only armed after drilling into a month from the Year view — otherwise Back behaves as it
    // always has (handled by the NavHost / Activity, outside this screen entirely).
    BackHandler(enabled = uiState.cameFromYear != null) { onBackFromYearDrillDown() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.view) {
            CalendarViewType.MONTH -> {
                val (title, subtitle) = monthHeaderTexts(visibleMonth)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp),
                ) {
                    CalendarHeader(
                        titleText    = title,
                        subtitleText = subtitle,
                        onPrevious   = onPreviousPeriod,
                        onNext       = onNextPeriod,
                        onTitleClick = { showGoToDate = true },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DotLegend(items = legendItems(uiState.gender, uiState.marks))
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
                        gender        = uiState.gender,
                        isSaving      = uiState.isSaving,
                        onPrayerClick = { type -> onPrayerClick(uiState.selectedDate, type) },
                        onStatusSave  = { type, status ->
                            onQuickStatusSave(uiState.selectedDate, type, status)
                        },
                    )

                    uiState.verse?.let { verse ->
                        Spacer(modifier = Modifier.height(20.dp))
                        VerseCard(verse)
                    }
                }
            }

            CalendarViewType.WEEK -> {
                val weekDates = weekDatesOf(uiState.selectedDate)
                val (title, subtitle) = weekHeaderTexts(weekDates.first(), weekDates.last())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp),
                ) {
                    CalendarHeader(
                        titleText    = title,
                        subtitleText = subtitle,
                        onPrevious   = onPreviousPeriod,
                        onNext       = onNextPeriod,
                        onTitleClick = { showGoToDate = true },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    DotLegend(items = legendItems(uiState.gender, uiState.marks))
                    Spacer(modifier = Modifier.height(20.dp))

                    WeekTable(
                        dates         = weekDates,
                        today         = uiState.today,
                        isLoading     = uiState.isLoadingMarks,
                        marks         = uiState.marks,
                        onPrayerClick = onPrayerClick,
                    )

                    uiState.verse?.let { verse ->
                        Spacer(modifier = Modifier.height(20.dp))
                        VerseCard(verse)
                    }
                }
            }

            CalendarViewType.YEAR -> {
                val year = uiState.selectedDate.year
                val (title, subtitle) = yearHeaderTexts(year)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 100.dp),
                ) {
                    CalendarHeader(
                        titleText    = title,
                        subtitleText = subtitle,
                        onPrevious   = onPreviousPeriod,
                        onNext       = onNextPeriod,
                        onTitleClick = { showGoToDate = true },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    DotLegend(items = yearLegendItems(uiState.gender, uiState.marks))
                    Spacer(modifier = Modifier.height(20.dp))

                    YearGrid(
                        year       = year,
                        today      = uiState.today,
                        isLoading  = uiState.isLoadingMarks,
                        marks      = uiState.marks,
                        onMonthClick = onMonthTileClick,
                    )
                }
            }
        }

        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily) }
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
            gender    = uiState.gender,
        )
    }
}

// ── Shared day-row building ───────────────────────────────────────────────────

/** Builds the 5-row list [SelectedDayPanel] renders, resolving each prayer's time label and
 * markability. */
@Composable
private fun buildDayDetailRows(
    date: LocalDate,
    today: LocalDate,
    statuses: Map<PrayerType, PrayerStatus>,
    times: Map<PrayerType, LocalTime>?,
): List<DayDetailPrayerRow> {
    val use24HourFormat = LocalUse24HourFormat.current
    val activePrayer = remember(date, today, times) {
        if (date == today) currentPrayerOf(times, LocalTime.now()) else null
    }
    return PrayerType.values().map { type ->
        val time = times?.get(type)
        DayDetailPrayerRow(
            type       = type,
            timeLabel  = time?.formatAppTime(use24HourFormat) ?: "--:--",
            status     = statuses[type] ?: PrayerStatus.NOT_RECORDED,
            isMarkable = isPrayerMarkable(date, time, today),
            isActive   = type == activePrayer,
        )
    }
}

// ── Header: view chip, Today shortcut, chevrons + title/subtitle ──────────────
// titleText/subtitleText are computed per-view by the caller (monthHeaderTexts/weekHeaderTexts
// below) — this composable only lays them out, so it doesn't need to know which view it's in.

/** "October 2026" + the Hijri months that Gregorian month spans — the Month view's header. */
@Composable
private fun monthHeaderTexts(visibleMonth: YearMonth): Pair<String, String?> {
    val titleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val adjustment = LocalHijriAdjustmentDays.current
    val startHijri = remember(visibleMonth, adjustment) { visibleMonth.atDay(1).toHijriOrNull(adjustment) }
    val endHijri = remember(visibleMonth, adjustment) { visibleMonth.atEndOfMonth().toHijriOrNull(adjustment) }
    val subtitle = if (startHijri != null && endHijri != null) hijriMonthRangeLabel(startHijri, endHijri) else null
    return visibleMonth.format(titleFormatter) to subtitle
}

/** "11 – 17 Oct 2026" (or "28 Sep – 4 Oct 2026" / "29 Dec 2025 – 4 Jan 2026" across a month or
 * year boundary) + the Hijri months the week spans. */
@Composable
private fun weekHeaderTexts(start: LocalDate, end: LocalDate): Pair<String, String?> {
    val dayMonth = remember { DateTimeFormatter.ofPattern("d MMM") }
    val dayMonthYear = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val title = if (start.year != end.year) {
        "${start.format(dayMonthYear)} – ${end.format(dayMonthYear)}"
    } else {
        "${start.format(dayMonth)} – ${end.format(dayMonthYear)}"
    }
    val adjustment = LocalHijriAdjustmentDays.current
    val startHijri = remember(start, adjustment) { start.toHijriOrNull(adjustment) }
    val endHijri = remember(end, adjustment) { end.toHijriOrNull(adjustment) }
    val subtitle = if (startHijri != null && endHijri != null) hijriMonthRangeLabel(startHijri, endHijri) else null
    return title to subtitle
}

/** "2026" + the Hijri years it spans — the Year view's header. */
@Composable
private fun yearHeaderTexts(year: Int): Pair<String, String?> {
    val adjustment = LocalHijriAdjustmentDays.current
    val startHijri = remember(year, adjustment) { LocalDate.of(year, 1, 1).toHijriOrNull(adjustment) }
    val endHijri = remember(year, adjustment) { LocalDate.of(year, 12, 31).toHijriOrNull(adjustment) }
    val subtitle = if (startHijri != null && endHijri != null) hijriYearRangeLabel(startHijri.year, endHijri.year) else null
    return year.toString() to subtitle
}

/** Just the chevron/title/subtitle row now — the view switcher and Today shortcut moved to
 * the shared top bar (MainActivity's TopBar), which drives CalendarViewModel directly. */
@Composable
private fun CalendarHeader(
    titleText: String,
    subtitleText: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
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
                text       = titleText,
                color      = SalatPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppTitleFontFamily,
                textAlign  = TextAlign.Center,
            )
            if (subtitleText != null) {
                Text(
                    text       = subtitleText,
                    color      = SalatOnSurfaceVariant,
                       fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                    textAlign  = TextAlign.Center,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.bulk_next), tint = SalatOnSurfaceVariant)
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

private data class LegendItem(val color: Color, val labelRes: Int)

// Built fresh every call rather than cached in a top-level val — the SalatStatusXxx colors are
// theme-aware composable getters now, so these lists can't be computed once outside composition.
@Composable
private fun legendItems(gender: Gender?, marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>) = listOf(
    LegendItem(SalatStatusCongregation,  R.string.status_congregation),
    LegendItem(SalatStatusIndividual,    R.string.status_individual),
    LegendItem(SalatStatusQazaPending,   PrayerStatus.QAZA_PENDING.labelRes),
    LegendItem(SalatStatusQazaCompleted, PrayerStatus.QAZA_COMPLETED.labelRes),
    LegendItem(SalatStatusExcused,       PrayerStatus.MENSTRUATION.labelRes),
    LegendItem(SalatStatusNotRecorded,   R.string.status_not_recorded),
).filter { item ->
    val status = PrayerStatus.values().firstOrNull { it.labelRes == item.labelRes } ?: return@filter true
    status.isOfferedFor(gender) || marks.values.any { status in it.values }
}

/** Year view's tones are coarser than a single prayer's status — see [DayTone]/[toneColor]. */
@Composable
private fun yearLegendItems(gender: Gender?, marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>) = listOf(
    LegendItem(SalatStatusCongregation, R.string.year_tone_all_prayed),
    LegendItem(SalatStatusIndividual,   R.string.year_tone_partly),
    LegendItem(SalatStatusQazaPending,  PrayerStatus.QAZA_PENDING.labelRes),
    LegendItem(SalatStatusExcused,      PrayerStatus.MENSTRUATION.labelRes),
    LegendItem(SalatStatusNotRecorded,  R.string.status_not_recorded),
).filter { item ->
    val status = PrayerStatus.values().firstOrNull { it.labelRes == item.labelRes } ?: return@filter true
    status.isOfferedFor(gender) || marks.values.any { status in it.values }
}

@Composable
private fun DotLegend(items: List<LegendItem>) {
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
            items.chunked(3).forEach { row ->
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
        Text(stringResource(item.labelRes), color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium)
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
            if (verse.arabic.isNotBlank()) {
                Text(
                    text = verse.arabic,
                    color = SalatOnPrimary,
                    fontSize = 20.sp,
                    fontFamily = AmiriQuranFamily,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text       = "“${verse.text}”",
                color      = SalatOnPrimary,
                fontSize   = 14.sp,
                fontFamily = AppUiFontFamily,
                lineHeight = 20.sp,
            )
            if (verse.citation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text       = verse.citation,
                    color      = SalatOnPrimaryContainer,
                       fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
            }
        }
    }
}

// ── Shared grid pieces (Month view and Week table) ────────────────────────────

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
                      fontSize = 12.sp,
                fontWeight    = if (isFriday) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily    = AppUiFontFamily,
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

// ── Month ring grid ────────────────────────────────────────────────────────────

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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                dates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (isLoading) {
                                DayCellSkeleton(modifier = Modifier.weight(1f).height(RING_DAY_CELL_HEIGHT).padding(2.dp))
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

private val RING_DAY_CELL_HEIGHT = 64.dp

/** One grid cell: the 5-arc status ring around the Gregorian day number, with the Hijri day
 * number underneath. [isSelected] tints the whole cell for the Month view's inline day panel;
 * nothing currently passes true for the Week table (it has no such concept). */
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
    val adjustment = LocalHijriAdjustmentDays.current
    val hijriDay = remember(date, adjustment) { date.toHijriOrNull(adjustment)?.day }

    Column(
        modifier = modifier
            .height(RING_DAY_CELL_HEIGHT)
            .clip(RoundedCornerShape(10.dp))
            .let { if (isSelected) it.background(SalatPrimaryFixed.copy(alpha = 0.18f)) else it }
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            if (showRing) {
                PrayerStatusRing(statuses = statuses, modifier = Modifier.fillMaxSize().alpha(dimAlpha))
            }
            if (isToday) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SalatPrimary))
            }
            Text(
                text       = date.dayOfMonth.toString(),
                color      = if (isToday) SalatOnPrimary else SalatOnSurface,
                fontSize   = 13.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontFamily = AppUiFontFamily,
                modifier   = Modifier.alpha(if (isToday) 1f else dimAlpha),
            )
        }
        if (hijriDay != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = hijriDay.toString(),
                color      = if (hijriDay == 1) SalatPrimary else SalatOutline,
                   fontSize = 12.sp,
                fontWeight = if (hijriDay == 1) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = AppUiFontFamily,
                modifier   = Modifier.alpha(dimAlpha),
            )
        }
    }
}

@Composable
private fun DayCellSkeleton(modifier: Modifier = Modifier) {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val alpha = if (animationsEnabled) {
        val animated by rememberInfiniteTransition(label = "sk")
            .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
        animated
    } else {
        0.5f
    }
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
}

private fun statusSquareIconRes(status: PrayerStatus): Int = status.iconRes

/** White reads fine on every status colour except Individual's light amber. */
private fun statusSquareIconTint(status: PrayerStatus): Color =
    if (status == PrayerStatus.INDIVIDUAL) Color(0xFF412402) else Color.White

// ── Week table ────────────────────────────────────────────────────────────────
// Rows are prayers, columns are the 7 days of the selected week — tapping a square edits that
// one (date, prayer) directly through the same statusSheet mechanism everything else uses.
// Markability here is the coarse date-only rule (future days look disabled; today and earlier
// look interactive) — the exact "not due yet today" gating still happens in onPrayerClick
// itself, same as the Month view's inline rows and the day sheet.

private val WEEK_TABLE_LABEL_WIDTH = 44.dp

@Composable
private fun WeekTable(
    dates: List<LocalDate>,
    today: LocalDate,
    isLoading: Boolean,
    marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onPrayerClick: (LocalDate, PrayerType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(WEEK_TABLE_LABEL_WIDTH))
            dates.forEachIndexed { index, date ->
                val isFriday = index == FRIDAY_COLUMN_INDEX
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = stringResource(DAY_HEADERS[index]),
                        color      = if (isFriday) SalatPrimary else SalatOutline,
                           fontSize = 12.sp,
                        fontWeight = if (isFriday) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = AppUiFontFamily,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .let { if (date == today) it.background(SalatPrimary) else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = date.dayOfMonth.toString(),
                            color      = if (date == today) SalatOnPrimary else SalatOnSurface,
                            fontSize   = 12.sp,
                            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = AppUiFontFamily,
                        )
                    }
                }
            }
        }
        PrayerType.values().forEach { type ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = type.shortLabel(),
                    modifier   = Modifier.width(WEEK_TABLE_LABEL_WIDTH),
                    color      = SalatOnSurfaceVariant,
                       fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
                dates.forEach { date ->
                    if (isLoading) {
                        DayCellSkeleton(modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp))
                    } else {
                        WeekStatusSquare(
                            status   = marks[date]?.get(type),
                            enabled  = !date.isAfter(today),
                            onClick  = { onPrayerClick(date, type) },
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStatusSquare(
    status: PrayerStatus?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecorded = status != null && status != PrayerStatus.NOT_RECORDED
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(8.dp))
            .let { if (isRecorded) it.background(statusRingColor(status)) else it.border(1.5.dp, SalatOutlineVariant, RoundedCornerShape(8.dp)) }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isRecorded) {
            Icon(
                painter            = painterResource(statusSquareIconRes(status)),
                contentDescription = null,
                tint               = statusSquareIconTint(status),
                modifier           = Modifier.size(14.dp),
            )
        }
    }
}

// ── Year grid ─────────────────────────────────────────────────────────────────
// Twelve mini-months, three per row. Each day is a tinted square — the coarser 5-tone summary
// from DaySummary.kt, not a single prayer's status, since a whole year can't show 5 arcs per
// day legibly at this size. Tapping a month drills into it as the Month view (onMonthClick);
// Back out of that returns here via CalendarViewModel's cameFromYear + this screen's BackHandler.

@Composable
private fun toneColor(tone: DayTone): Color = when (tone) {
    DayTone.ALL_PRAYED   -> SalatStatusCongregation
    DayTone.PARTLY       -> SalatStatusIndividual
    DayTone.QAZA_PENDING -> SalatStatusQazaPending
    DayTone.EXCUSED      -> SalatStatusExcused
    DayTone.NOTHING      -> SalatStatusNotRecorded
}

@Composable
private fun YearGrid(
    year: Int,
    today: LocalDate,
    isLoading: Boolean,
    marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onMonthClick: (YearMonth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        (1..12).chunked(3).forEach { monthValues ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                monthValues.forEach { monthValue ->
                    val month = YearMonth.of(year, monthValue)
                    MiniMonthTile(
                        month     = month,
                        today     = today,
                        isLoading = isLoading,
                        marks     = marks,
                        onClick   = { onMonthClick(month) },
                        modifier  = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMonthTile(
    month: YearMonth,
    today: LocalDate,
    isLoading: Boolean,
    marks: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthNameFormatter = remember { DateTimeFormatter.ofPattern("MMM") }
    val isCurrentMonth = month == YearMonth.from(today)
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()

    Column(
        modifier            = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text       = month.format(monthNameFormatter),
            color      = if (isCurrentMonth) SalatPrimary else SalatOnSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = if (isCurrentMonth) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
            (0 until 6).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    (0 until 7).forEach { col ->
                        val dayNumber = row * 7 + col - firstDayOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            val tone = if (!isLoading && !date.isAfter(today)) {
                                dayToneOf(marks[date].orEmpty().values.toList())
                            } else {
                                null
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(tone?.let { toneColor(it) } ?: Color.Transparent),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Selected day panel ─────────────────────────────────────────────────────────

@Composable
private fun SelectedDayPanel(
    date: LocalDate,
    today: LocalDate,
    statuses: Map<PrayerType, PrayerStatus>,
    times: Map<PrayerType, LocalTime>?,
    gender: Gender?,
    isSaving: Boolean,
    onPrayerClick: (PrayerType) -> Unit,
    onStatusSave: (PrayerType, PrayerStatus) -> Unit,
) {
    val adjustment = LocalHijriAdjustmentDays.current
    val hijri = remember(date, adjustment) { date.toHijriOrNull(adjustment) }
    val islamicEvent = hijri?.islamicEventOrNull()
    val rows = buildDayDetailRows(date, today, statuses, times)

    Column(modifier = Modifier.fillMaxWidth()) {
        DayDetailHeader(
            date           = date,
            hijriLabel     = hijri?.fullLabel(),
            completedCount = completedCount(rows.map { it.status }),
            totalCount     = rows.size,
        )
        if (islamicEvent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            IslamicEventBanner(islamicEvent)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            rows.forEach { row ->
                DayDetailPrayerRowItem(
                    row = row,
                    gender = gender,
                    isSaving = isSaving,
                    onStatusSelected = { status -> onStatusSave(row.type, status) },
                    onClick = { if (row.isMarkable) onPrayerClick(row.type) },
                )
            }
        }
    }
}

/** The Islamic calendar observance (if any) landing on the selected day — see [IslamicEvent]. */
@Composable
private fun IslamicEventBanner(event: IslamicEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SalatPrimaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_mosque), null, tint = SalatOnPrimaryContainer, modifier = Modifier.size(16.dp))
        Text(event.label(), color = SalatOnPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

private fun sampleMarks(today: LocalDate): Map<LocalDate, Map<PrayerType, PrayerStatus>> = mapOf(
    today to mapOf(
        PrayerType.FAJR to PrayerStatus.CONGREGATION,
        PrayerType.DHUHR to PrayerStatus.INDIVIDUAL,
        PrayerType.ASR to PrayerStatus.CONGREGATION,
        PrayerType.MAGHRIB to PrayerStatus.QAZA_PENDING,
    ),
    today.minusDays(1) to PrayerType.values().associateWith { PrayerStatus.CONGREGATION },
)

private fun sampleUiState(view: CalendarViewType): CalendarUiState {
    val today = LocalDate.now()
    return CalendarUiState(
        view           = view,
        today          = today,
        selectedDate   = today,
        isLoadingMarks = false,
        marks          = sampleMarks(today),
        selectedTimes  = null,
        verse = VerseInfo(
            text     = "Indeed, prayer prohibits immorality and wrongdoing, and the remembrance of Allāh is greater.",
            citation = "Al-Ankabut 29:45",
        ),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenMonthPreview() = PreviewCalendarScreen(sampleUiState(CalendarViewType.MONTH))

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenWeekPreview() = PreviewCalendarScreen(sampleUiState(CalendarViewType.WEEK))

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenYearPreview() = PreviewCalendarScreen(sampleUiState(CalendarViewType.YEAR))

@Composable
private fun PreviewCalendarScreen(uiState: CalendarUiState) {
    CalendarScreen(
        uiState                 = uiState,
        onPreviousPeriod        = {},
        onNextPeriod            = {},
        onDatePicked            = {},
        onDaySelected           = {},
        onMonthTileClick        = {},
        onBackFromYearDrillDown = {},
        onPrayerClick           = { _, _ -> },
        onQuickStatusSave       = { _, _, _ -> },
        onStatusSheetDismiss    = {},
        onStatusSave            = {},
        onErrorDismiss          = {},
    )
}
