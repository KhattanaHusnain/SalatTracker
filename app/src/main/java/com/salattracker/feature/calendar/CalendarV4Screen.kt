package com.salattracker.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatBackground
import com.salattracker.core.theme.SalatError
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatStatusNotRecorded
import com.salattracker.core.theme.SalatSurface
import com.salattracker.core.theme.SalatSurfaceContainerLow
import com.salattracker.core.ui.CompactPrayerRow
import com.salattracker.core.ui.CompactPrayerStatusActions
import com.salattracker.core.ui.DatePickerModal
import com.salattracker.core.ui.LocalHijriAdjustmentDays
import com.salattracker.core.ui.PrayerStatusRing
import com.salattracker.core.ui.compactPrayerStatusLabel
import com.salattracker.core.ui.fullLabel
import com.salattracker.core.ui.hijriMonthRangeLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.shortLabelRes
import com.salattracker.core.ui.statusRingColor
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerSlot
import com.salattracker.domain.usecase.toHijriOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarV4Screen(
    state: CalendarV4UiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayTapped: (LocalDate) -> Unit,
    onQazaOnly: () -> Unit,
    onPrayerFilter: (PrayerType?) -> Unit,
    onEditToggle: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectRange: (LocalDate, LocalDate) -> Unit,
    onCatchUp: () -> Unit,
    onPrepareBatch: (PrayerStatus) -> Unit,
    onFillEmptyOnly: (Boolean) -> Unit,
    onDismissPreview: () -> Unit,
    onApplyPreview: () -> Unit,
    onUndo: () -> Unit,
    onDismissDay: () -> Unit,
    onSaveDayStatus: (PrayerType, PrayerStatus) -> Unit,
    onErrorDismiss: () -> Unit,
    onOpenQaza: () -> Unit,
) {
    var showRange by remember { mutableStateOf(false) }
    val monthDates = remember(state.visibleMonth) { calendarMonthGridDates(state.visibleMonth) }
    val monthEntries = remember(state.entries, state.visibleMonth, state.prayerFilter) {
        state.entries.filterKeys { YearMonth.from(it) == state.visibleMonth }
    }
    val filteredEntries = monthEntries.values.flatMap { it.values }
        .filter { state.prayerFilter == null || it.prayerType == state.prayerFilter }
    val visibleEntries = filteredEntries.filter {
        !state.qazaOnly || it.status in listOf(PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED)
    }
    val trackedDays = (1..state.visibleMonth.lengthOfMonth()).map(state.visibleMonth::atDay)
        .count { it in state.trackingStart..state.today }
    val totalSlots = trackedDays * (if (state.prayerFilter == null) 5 else 1)
    val notRecorded = totalSlots - filteredEntries.count { it.status != PrayerStatus.NOT_RECORDED }
    val qazaCount = visibleEntries.count { it.status == PrayerStatus.QAZA_PENDING }

    Box(Modifier.fillMaxSize().background(SalatBackground)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = if (state.isEditMode) 190.dp else 104.dp),
        ) {
            CalendarPeriodHeader(state, onPreviousMonth, onNextMonth)
            FilterRow(state, onQazaOnly, onPrayerFilter)
            Spacer(Modifier.height(8.dp))
            CalendarMonthGrid(state, monthDates, onDayTapped)
            Spacer(Modifier.height(12.dp))
            StatusSummary(visibleEntries, state.gender)
            if (!state.isEditMode && !state.qazaOnly && notRecorded >= 3) {
                SummaryAction(
                    text = stringResource(R.string.calendar_v4_not_recorded_summary, notRecorded.coerceAtLeast(0)),
                    action = stringResource(R.string.calendar_v4_catch_up),
                    onClick = onCatchUp,
                )
            }
            if (!state.isEditMode && qazaCount > 0) {
                SummaryAction(
                    text = stringResource(R.string.calendar_v4_owed_summary, qazaCount),
                    action = stringResource(R.string.calendar_v4_make_up_qaza),
                    onClick = onOpenQaza,
                )
            }
        }

        if (state.isEditMode) {
            CalendarEditBar(
                state = state,
                onDone = onEditToggle,
                onClear = onClearSelection,
                onRange = { showRange = true },
                onStatus = onPrepareBatch,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        state.undoCount?.let { count ->
            Snackbar(
                modifier = Modifier.align(if (state.daySheetDate != null) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(16.dp).padding(bottom = if (state.isEditMode) 156.dp else 72.dp),
                action = { TextButton(onClick = onUndo) { Text(stringResource(R.string.calendar_v4_undo)) } },
            ) { Text(stringResource(R.string.calendar_v4_updated, count)) }
        }
        state.errorRes?.let { error ->
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp), action = {
                TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok)) }
            }) { Text(stringResource(error)) }
        }
    }

    if (showRange) {
        CalendarRangeDialog(
            startBoundary = state.trackingStart,
            endBoundary = state.today,
            onDismiss = { showRange = false },
            onApply = { from, to -> onSelectRange(from, to); showRange = false },
        )
    }
    state.preview?.let { preview ->
        BatchPreviewDialog(state, preview, onFillEmptyOnly, onDismissPreview, onApplyPreview)
    }
    state.daySheetDate?.let { date ->
        CalendarDaySheet(state, date, onDismissDay, onSaveDayStatus)
    }
}

@Composable
private fun CalendarPeriodHeader(state: CalendarV4UiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val start = state.visibleMonth.atDay(1).toHijriOrNull(LocalHijriAdjustmentDays.current)
    val end = state.visibleMonth.atEndOfMonth().toHijriOrNull(LocalHijriAdjustmentDays.current)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = state.canGoPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.bulk_previous))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.visibleMonth.format(formatter), fontFamily = AppTitleFontFamily, fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold, color = SalatOnSurface, maxLines = 1)
            if (start != null && end != null) Text(hijriMonthRangeLabel(start, end), fontFamily = AppUiFontFamily,
                fontSize = 12.sp, color = SalatOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onNext, enabled = state.canGoNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.bulk_next))
        }
    }
}

@Composable
private fun FilterRow(state: CalendarV4UiState, onQazaOnly: () -> Unit, onPrayerFilter: (PrayerType?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("calendar-filter-row"),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(stringResource(R.string.calendar_v4_qaza_only), state.qazaOnly, onQazaOnly)
        FilterChip(stringResource(R.string.calendar_v4_all), state.prayerFilter == null) { onPrayerFilter(null) }
        PrayerType.values().forEach { type -> FilterChip(type.label(), state.prayerFilter == type) { onPrayerFilter(type) } }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape,
        color = if (selected) Color(0xFFEAF4F3) else SalatSurface,
        border = BorderStroke(1.5.dp, if (selected) SalatPrimary else SalatOutlineVariant)) {
        Box(Modifier.height(40.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontFamily = AppUiFontFamily, fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) SalatPrimary else SalatOnSurface, maxLines = 1)
        }
    }
}

private val mondayHeaders = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

@Composable
private fun CalendarMonthGrid(state: CalendarV4UiState, dates: List<LocalDate>, onDayTapped: (LocalDate) -> Unit) {
    Column(Modifier.testTag("calendar-month-grid"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            mondayHeaders.forEach { day ->
                Text(stringResource(day.shortLabelRes), Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontFamily = AppUiFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (day == DayOfWeek.FRIDAY) SalatPrimary else SalatOnSurfaceVariant)
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDayCell(state, date, onDayTapped, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(state: CalendarV4UiState, date: LocalDate, onClick: (LocalDate) -> Unit, modifier: Modifier) {
    val currentMonth = YearMonth.from(date) == state.visibleMonth
    val enabled = date in state.trackingStart..state.today
    val entries = state.entries[date].orEmpty()
    val statuses = PrayerType.values().associateWith { type ->
        val status = entries[type]?.status ?: PrayerStatus.NOT_RECORDED
        when {
            state.qazaOnly && status !in listOf(PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED) -> PrayerStatus.NOT_RECORDED
            state.prayerFilter != null && type != state.prayerFilter -> PrayerStatus.NOT_RECORDED
            else -> status
        }
    }
    val selected = state.selectedSlots.any { it.date == date }
    val hijri = date.toHijriOrNull(LocalHijriAdjustmentDays.current)
    Column(
        modifier.height(62.dp).padding(1.dp).clip(RoundedCornerShape(9.dp))
            .background(if (selected) SalatPrimary.copy(alpha = .10f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 0.dp, if (selected) SalatPrimary else Color.Transparent,
                RoundedCornerShape(9.dp))
            .clickable(enabled = enabled) { onClick(date) }.alpha(if (currentMonth && enabled) 1f else .35f)
            .testTag("calendar-day-$date"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (!date.isAfter(state.today)) PrayerStatusRing(statuses, Modifier.fillMaxSize(), strokeFraction = .13f)
            if (date == state.today) TodayPulseRing()
            Text(date.dayOfMonth.toString(), fontFamily = AppUiFontFamily, fontSize = 12.sp,
                fontWeight = if (date == state.today) FontWeight.SemiBold else FontWeight.Normal,
                color = if (date == state.today) Color(0xFFB07F1E) else SalatOnSurface)
        }
        Text(
            text = hijri?.let { if (it.day == 1) "${it.day}/${it.month}" else it.day.toString() } ?: "",
            fontFamily = AppUiFontFamily, fontSize = 9.sp,
            color = when {
                date == state.today -> Color(0xFFB07F1E)
                hijri?.day == 1 -> SalatPrimary
                else -> SalatOnSurfaceVariant
            }, maxLines = 1,
        )
    }
}

@Composable
private fun TodayPulseRing() {
    val alpha = rememberInfiniteTransition(label = "calendar-today-pulse").animateFloat(
        initialValue = .35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "calendar-today-alpha",
    ).value
    val gold = Color(0xFFD9A441)
    Box(
        Modifier.size(40.dp)
            .drawBehind { drawCircle(gold.copy(alpha = .16f * alpha), radius = size.minDimension * .58f) }
            .border(1.6.dp, gold.copy(alpha = .70f + .30f * alpha), CircleShape),
    )
}

@Composable
private fun StatusSummary(entries: List<PrayerEntry>, gender: Gender?) {
    val statuses = if (gender == Gender.FEMALE) listOf(PrayerStatus.INDIVIDUAL, PrayerStatus.MENSTRUATION,
        PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED) else listOf(PrayerStatus.CONGREGATION,
        PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED)
    val total = entries.size.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)) {
        statuses.forEach { status ->
            val count = entries.count { it.status == status }
            if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxSize().background(statusRingColor(status)))
        }
        val empty = (total - entries.count { it.status != PrayerStatus.NOT_RECORDED }).coerceAtLeast(0)
        if (empty > 0) Box(Modifier.weight(empty.toFloat()).fillMaxSize().background(SalatStatusNotRecorded))
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statuses.forEach { status ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(status.iconRes), null, tint = statusRingColor(status), modifier = Modifier.size(14.dp))
                Text("${compactPrayerStatusLabel(status, gender)} ${entries.count { it.status == status }}",
                    fontFamily = AppUiFontFamily, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SummaryAction(text: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), fontFamily = AppUiFontFamily, fontSize = 13.sp, color = SalatOnSurfaceVariant)
        TextButton(onClick = onClick) { Text(action, fontFamily = AppUiFontFamily, fontSize = 13.sp) }
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

@Composable
private fun CalendarEditBar(
    state: CalendarV4UiState,
    onDone: () -> Unit,
    onClear: () -> Unit,
    onRange: () -> Unit,
    onStatus: (PrayerStatus) -> Unit,
    modifier: Modifier,
) {
    val compact = LocalDensity.current.fontScale > 1.18f || LocalConfiguration.current.screenWidthDp < 360
    Surface(modifier.fillMaxWidth().navigationBarsPadding().testTag("calendar-edit-bar"), shadowElevation = 8.dp,
        color = SalatSurface, border = BorderStroke(1.dp, SalatOutlineVariant)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            val selectionText = if (state.selectedSlots.isEmpty()) stringResource(R.string.calendar_v4_tap_days)
                else stringResource(R.string.calendar_v4_selected, state.selectedSlots.size)
            if (compact) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectionText, Modifier.weight(1f), fontFamily = AppUiFontFamily, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = onDone) { Text(stringResource(R.string.calendar_v4_done)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (state.selectedSlots.isNotEmpty()) TextButton(onClick = onClear) { Text(stringResource(R.string.calendar_v4_clear)) }
                    TextButton(onClick = onRange) { Text(stringResource(R.string.calendar_v4_select_range)) }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectionText, Modifier.weight(1f), fontFamily = AppUiFontFamily, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.selectedSlots.isNotEmpty()) TextButton(onClick = onClear) { Text(stringResource(R.string.calendar_v4_clear)) }
                    TextButton(onClick = onRange) { Text(stringResource(R.string.calendar_v4_select_range)) }
                    TextButton(onClick = onDone) { Text(stringResource(R.string.calendar_v4_done)) }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                offeredBulkStatuses(state.gender).forEach { status ->
                    OutlinedButton(onClick = { onStatus(status) }, enabled = state.selectedSlots.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(40.dp)) {
                        Icon(painterResource(status.iconRes), null, Modifier.size(16.dp), tint = statusRingColor(status))
                        Spacer(Modifier.width(4.dp)); Text(compactPrayerStatusLabel(status, state.gender), maxLines = 1, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun offeredBulkStatuses(gender: Gender?) = buildList {
    if (gender != Gender.FEMALE) add(PrayerStatus.CONGREGATION)
    add(PrayerStatus.INDIVIDUAL)
    if (gender != Gender.MALE) add(PrayerStatus.MENSTRUATION)
    add(PrayerStatus.QAZA_PENDING)
    add(PrayerStatus.QAZA_COMPLETED)
    add(PrayerStatus.NOT_RECORDED)
}

@Composable
private fun BatchPreviewDialog(
    state: CalendarV4UiState,
    preview: CalendarBatchPreview,
    onFill: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_v4_preview), fontFamily = AppTitleFontFamily) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(compactPrayerStatusLabel(preview.status, state.gender), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.calendar_v4_overwrite, preview.overwriteCount), color = SalatOnSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.calendar_v4_fill_empty), Modifier.weight(1f))
                Switch(state.fillEmptyOnly, onFill)
            }
        } },
        confirmButton = { Button(onClick = onApply, enabled = preview.updates.isNotEmpty() && !state.isSaving) {
            Text(stringResource(R.string.calendar_v4_apply, preview.updates.size))
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun CalendarRangeDialog(
    startBoundary: LocalDate,
    endBoundary: LocalDate,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit,
) {
    var from by remember { mutableStateOf(startBoundary) }
    var to by remember { mutableStateOf(endBoundary) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_v4_range_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ pickingFrom = true }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_v4_from, from.toString()))
            }
            OutlinedButton({ pickingTo = true }, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.calendar_v4_to, to.toString()))
            }
        } },
        confirmButton = { Button({ onApply(from, to) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (pickingFrom) DatePickerModal(from, { from = it; pickingFrom = false }, { pickingFrom = false }, startBoundary, endBoundary)
    if (pickingTo) DatePickerModal(to, { to = it; pickingTo = false }, { pickingTo = false }, startBoundary, endBoundary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDaySheet(
    state: CalendarV4UiState,
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (PrayerType, PrayerStatus) -> Unit,
) {
    val entries = state.entries[date].orEmpty()
    val hijri = date.toHijriOrNull(LocalHijriAdjustmentDays.current)
    val now = LocalTime.now()
    val currentPrayer = if (date == state.today) state.selectedTimes.orEmpty()
        .filterValues { !now.isBefore(it) }.maxByOrNull { it.value }?.key else null
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurface,
        modifier = Modifier.testTag("calendar-day-sheet")) {
        Column(Modifier.fillMaxWidth()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
            .verticalScroll(rememberScrollState())
            .testTag("calendar-day-sheet-content")
            .padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            Text(hijri?.fullLabel().orEmpty(), fontFamily = AppUiFontFamily, fontSize = 13.sp, color = SalatPrimary)
            Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), fontFamily = AppTitleFontFamily,
                fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = SalatOnSurface)
            Spacer(Modifier.height(8.dp))
            PrayerType.values().forEach { type ->
                val entry = entries[type]
                val status = entry?.status ?: PrayerStatus.NOT_RECORDED
                val time = state.selectedTimes?.get(type)
                val markable = date < state.today || (date == state.today && time != null && !LocalTime.now().isBefore(time))
                DaySheetPrayerRow(date, type, time, entry, status, state.gender, markable,
                    isActive = type == currentPrayer, saving = state.isSaving) { onSave(type, it) }
            }
        }
    }
}

@Composable
private fun DaySheetPrayerRow(
    date: LocalDate,
    type: PrayerType,
    time: LocalTime?,
    entry: PrayerEntry?,
    status: PrayerStatus,
    gender: Gender?,
    markable: Boolean,
    isActive: Boolean,
    saving: Boolean,
    onSave: (PrayerStatus) -> Unit,
) {
    val stacked = LocalDensity.current.fontScale > 1.18f || LocalConfiguration.current.screenWidthDp < 340
    CompactPrayerRow(type, markable, SalatPrimary, SalatOutlineVariant, SalatOnSurfaceVariant,
        modifier = Modifier.testTag("calendar-sheet-prayer-row-$type"), isActive = isActive,
        content = {
            Text(if (type == PrayerType.DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY && gender != Gender.FEMALE)
                stringResource(R.string.prayer_jumuah) else type.label(), fontFamily = AppUiFontFamily,
                fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "—", fontFamily = AppUiFontFamily,
                fontSize = 12.sp, color = SalatOnSurfaceVariant)
            Text(when {
                !markable -> stringResource(R.string.calendar_v4_not_yet)
                entry?.loggedLate == true -> stringResource(R.string.calendar_v4_logged_late)
                entry != null && status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL) -> stringResource(R.string.calendar_v4_on_time)
                else -> ""
            }, fontFamily = AppUiFontFamily, fontSize = 11.sp, color = SalatOnSurfaceVariant)
            if (status == PrayerStatus.QAZA_PENDING) TextButton(onClick = { onSave(PrayerStatus.QAZA_COMPLETED) }, enabled = !saving) {
                Text(stringResource(R.string.home_v8_make_up), fontSize = 11.sp)
            }
        },
        actions = { if (!stacked) CompactPrayerStatusActions(type, status, gender, markable && !saving, "calendar-sheet", onSave) },
        footer = { if (stacked) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CompactPrayerStatusActions(type, status, gender, markable && !saving, "calendar-sheet", onSave)
        } },
    )
}
