package com.salattracker.feature.bulkupdate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.DatePickerModal
import com.salattracker.core.ui.isOfferedFor
import com.salattracker.core.ui.label
import com.salattracker.core.ui.labelRes
import com.salattracker.core.ui.nameRes
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Bulk Update Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkUpdateScreen(
    uiState: BulkUpdateUiState,
    onDurationChanged: (DurationOption) -> Unit,
    onPreviousRange: () -> Unit,
    onNextRange: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onCustomRangeChanged: (LocalDate, LocalDate) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    onSelectAllToggle: () -> Unit,
    onStatusChanged: (PrayerStatus) -> Unit,
    onApplyClick: () -> Unit,
    onConfirmApply: () -> Unit,
    onDismissConfirm: () -> Unit,
    onClearApplySuccess: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Duration tabs ──────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.bulk_select_duration))
            DurationTabs(selected = uiState.duration, onSelected = onDurationChanged)

            // ── Date range card ────────────────────────────────────────────────
            DateRangeCard(
                duration    = uiState.duration,
                rangeFrom   = uiState.rangeFrom,
                rangeTo     = uiState.effectiveRangeTo,
                canGoNext     = uiState.canGoNext,
                onPickStart   = { pickingStart = true },
                onPickEnd     = { pickingEnd = true },
                onPrevious    = onPreviousRange,
                onNext        = onNextRange,
                onDaySelected = onDaySelected,
            )

            // ── Prayer multi-select ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                SectionLabel(stringResource(R.string.bulk_select_prayers), modifier = Modifier)
                val allSelected = uiState.selectedPrayers.size == PrayerType.values().size
                TextButton(onClick = onSelectAllToggle) {
                    Text(
                        text = stringResource(if (allSelected) R.string.action_deselect_all else R.string.action_select_all),
                        color = SalatPrimary,
                        fontFamily = AppUiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrayerType.values().forEach { type ->
                    PrayerCheckRow(
                        type     = type,
                        checked  = type in uiState.selectedPrayers,
                        onToggle = { onTogglePrayer(type) },
                    )
                }
            }

            // ── Status selector ────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.bulk_set_status))
            StatusChipRow(selected = uiState.selectedStatus, gender = uiState.gender, onSelected = onStatusChanged)

            // ── Apply button ───────────────────────────────────────────────────
            Button(
                onClick  = onApplyClick,
                enabled  = uiState.selectedPrayers.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SalatPrimary,
                    contentColor   = SalatOnPrimary,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.bulk_apply_changes), fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    val prayersPart = pluralStringResource(R.plurals.bulk_prayers_count, uiState.selectedPrayers.size, uiState.selectedPrayers.size)
                    val daysPart = pluralStringResource(R.plurals.bulk_days_count, uiState.dayCount, uiState.dayCount)
                    Text(
                        text = stringResource(
                            R.string.bulk_apply_summary,
                            stringResource(uiState.selectedStatus.labelRes).uppercase(),
                            "$prayersPart $daysPart",
                        ),
                        fontFamily = AppUiFontFamily,
                        fontSize   = 10.sp,
                        letterSpacing = 1.sp,
                        color      = SalatPrimaryFixed.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // ── Success / error snackbars ──────────────────────────────────────────
        if (uiState.applySuccess) {
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onClearApplySuccess) { Text(stringResource(R.string.action_ok), color = SalatPrimary) } },
                containerColor = SalatSecondaryContainer,
                contentColor   = SalatOnSecondaryContainer,
            ) {
                Text(
                    text = if (uiState.skippedFutureCount > 0)
                        stringResource(R.string.bulk_applied_skipped, uiState.skippedFutureCount)
                    else
                        stringResource(R.string.bulk_applied),
                    fontFamily = AppUiFontFamily,
                )
            }
        }
        uiState.error?.let { msg ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(msg, fontFamily = AppUiFontFamily) }
        }
    }

    // ── Confirmation dialog ────────────────────────────────────────────────────
    if (uiState.showConfirmDialog) {
        BulkUpdateConfirmDialog(
            uiState   = uiState,
            onConfirm = onConfirmApply,
            onDismiss = onDismissConfirm,
        )
    }

    // ── Custom range date pickers ──────────────────────────────────────────────
    if (pickingStart) {
        DatePickerModal(
            initialDate = uiState.rangeFrom,
            onDatePicked = { picked ->
                onCustomRangeChanged(picked, uiState.rangeTo)
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingEnd) {
        DatePickerModal(
            initialDate = uiState.rangeTo,
            onDatePicked = { picked ->
                onCustomRangeChanged(uiState.rangeFrom, picked)
                pickingEnd = false
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Text(
        text          = text.uppercase(),
        modifier      = modifier,
        color         = SalatOnSurfaceVariant,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        fontFamily    = AppUiFontFamily,
        letterSpacing = 1.5.sp,
    )
}

// ── Duration tabs ────────────────────────────────────────────────────────────

@Composable
private fun DurationTabs(selected: DurationOption, onSelected: (DurationOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SalatSurfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DurationOption.values().forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick  = { onSelected(option) },
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp),
                color    = if (isSelected) SalatSurfaceContainerLowest else Color.Transparent,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
            ) {
                Text(
                    text       = stringResource(option.labelRes),
                    modifier   = Modifier.padding(vertical = 10.dp),
                    textAlign  = TextAlign.Center,
                    color      = if (isSelected) SalatPrimary else SalatOnSurfaceVariant,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppUiFontFamily,
                )
            }
        }
    }
}

// ── Date range card ──────────────────────────────────────────────────────────

private val rangeLabelFormatter = DateTimeFormatter.ofPattern("MMM d")
private val dayLabelFormatter   = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

private fun rangeDisplayLabel(duration: DurationOption, rangeFrom: LocalDate, rangeTo: LocalDate): String = when (duration) {
    DurationOption.DAY   -> rangeFrom.format(dayLabelFormatter)
    DurationOption.MONTH -> rangeFrom.format(monthLabelFormatter)
    else                 -> "${rangeFrom.format(rangeLabelFormatter)} — ${rangeTo.format(rangeLabelFormatter)}"
}

@Composable
private fun DateRangeCard(
    duration: DurationOption,
    rangeFrom: LocalDate,
    rangeTo: LocalDate,
    canGoNext: Boolean,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
) {
    val isCustom = duration == DurationOption.CUSTOM

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.bulk_date_range), color = SalatOutline, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                    Text(
                        text       = rangeDisplayLabel(duration, rangeFrom, rangeTo),
                        color      = SalatOnSurface,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppTitleFontFamily,
                    )
                }
                if (isCustom) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SalatSecondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_calendar_today), null, tint = SalatOnSecondaryContainer)
                    }
                } else {
                    Row {
                        IconButton(onClick = onPrevious) {
                            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.bulk_previous), tint = SalatOnSurfaceVariant)
                        }
                        IconButton(onClick = onNext, enabled = canGoNext) {
                            Icon(
                                painterResource(R.drawable.ic_chevron_right),
                                stringResource(R.string.bulk_next),
                                tint = if (canGoNext) SalatOnSurfaceVariant else SalatOutlineVariant,
                            )
                        }
                    }
                }
            }

            if (isCustom) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onPickStart, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.bulk_start, rangeFrom.format(rangeLabelFormatter)), fontFamily = AppUiFontFamily, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onPickEnd, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.bulk_end, rangeTo.format(rangeLabelFormatter)), fontFamily = AppUiFontFamily, fontSize = 12.sp)
                    }
                }
            }

            MiniCalendarGrid(
                rangeFrom = rangeFrom,
                rangeTo   = rangeTo,
                onDayClick = if (duration == DurationOption.DAY) onDaySelected else null,
            )
        }
    }
}

private val MINI_DAY_HEADERS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
private fun MiniCalendarGrid(
    rangeFrom: LocalDate,
    rangeTo: LocalDate,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val gridStart = rangeFrom.minusDays(((rangeFrom.dayOfWeek.value % 7).toLong()))
    val gridEndRaw = rangeTo.plusDays((6 - (rangeTo.dayOfWeek.value % 7)).toLong())
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(gridStart, gridEndRaw).toInt() + 1
    val weeks = (totalDays / 7).coerceIn(1, 6)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MINI_DAY_HEADERS.forEach { label ->
                Text(
                    text      = label,
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color     = SalatOutline,
                    fontSize  = 10.sp,
                    fontFamily = AppUiFontFamily,
                )
            }
        }
        repeat(weeks) { weekIdx ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIdx ->
                    val date = gridStart.plusDays((weekIdx * 7 + dayIdx).toLong())
                    val isFuture = date.isAfter(LocalDate.now())
                    val inRange = !date.isBefore(rangeFrom) && !date.isAfter(rangeTo)
                    val isEndpoint = date == rangeFrom || date == rangeTo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .let { m -> if (onDayClick != null && !isFuture) m.clickable { onDayClick(date) } else m }
                            .background(
                                when {
                                    isEndpoint -> SalatPrimary
                                    inRange    -> SalatPrimaryFixed
                                    else       -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = date.dayOfMonth.toString(),
                            color = when {
                                isEndpoint -> SalatOnPrimary
                                inRange    -> SalatPrimary
                                isFuture   -> SalatOutlineVariant
                                else       -> SalatOutline
                            },
                            fontSize = 11.sp,
                            fontFamily = AppUiFontFamily,
                        )
                    }
                }
            }
        }
    }
}

// ── Prayer checkbox row ───────────────────────────────────────────────────────

@Composable
private fun PrayerCheckRow(type: PrayerType, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick  = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(type.iconRes), null, tint = SalatPrimary)
                Text(type.label(), color = SalatOnSurface, fontSize = 16.sp, fontFamily = AppUiFontFamily)
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = SalatPrimaryContainer),
            )
        }
    }
}

// ── Status chip row ──────────────────────────────────────────────────────────

private data class StatusChipItem(val status: PrayerStatus, val iconRes: Int)

private val STATUS_CHIPS = listOf(
    StatusChipItem(PrayerStatus.CONGREGATION,   R.drawable.ic_groups),
    StatusChipItem(PrayerStatus.INDIVIDUAL,     R.drawable.ic_person),
    StatusChipItem(PrayerStatus.QAZA_PENDING,   R.drawable.ic_pending_actions),
    StatusChipItem(PrayerStatus.QAZA_COMPLETED, R.drawable.ic_history_edu),
    StatusChipItem(PrayerStatus.MENSTRUATION,   R.drawable.ic_female),
)

@Composable
private fun StatusChipRow(selected: PrayerStatus, gender: Gender?, onSelected: (PrayerStatus) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STATUS_CHIPS.filter { it.status.isOfferedFor(gender) }.forEach { chip ->
            val isSelected = chip.status == selected
            Surface(
                onClick = { onSelected(chip.status) },
                shape   = RoundedCornerShape(50),
                color   = if (isSelected) SalatPrimaryContainer else SalatSurface,
                border  = BorderStroke(2.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter  = painterResource(chip.iconRes),
                        contentDescription = null,
                        tint     = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text       = chip.status.label(),
                        color      = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
                        fontSize   = 12.sp,
                        fontFamily = AppUiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Confirmation dialog ───────────────────────────────────────────────────────

@Composable
private fun BulkUpdateConfirmDialog(
    uiState: BulkUpdateUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SalatSurfaceContainerLowest,
        shape            = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SalatPrimaryContainer.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_history_edu), null, tint = SalatPrimary)
            }
        },
        title = {
            Text(stringResource(R.string.bulk_confirm_title), color = SalatOnSurface, fontFamily = AppTitleFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val context = LocalContext.current
                val prayerNames = uiState.selectedPrayers.sortedBy { it.ordinal }.joinToString(", ") { context.getString(it.nameRes) }
                val daysPart = pluralStringResource(R.plurals.bulk_days_count, uiState.dayCount, uiState.dayCount)
                val range = "${uiState.rangeFrom.format(rangeLabelFormatter)} – ${uiState.effectiveRangeTo.format(rangeLabelFormatter)}"
                Text(
                    text = stringResource(
                        R.string.bulk_confirm_summary,
                        stringResource(uiState.selectedStatus.labelRes),
                        "$prayerNames ($range, $daysPart)",
                    ),
                    color = SalatOnSurfaceVariant,
                    fontSize = 15.sp,
                    fontFamily = AppUiFontFamily,
                    lineHeight = 22.sp,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SalatErrorContainer.copy(alpha = 0.4f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_pending_actions), null, tint = SalatError, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.bulk_confirm_desc),
                            color = SalatOnErrorContainer,
                            fontSize = 11.sp,
                            fontFamily = AppUiFontFamily,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = !uiState.isApplying,
                colors   = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                shape    = RoundedCornerShape(50),
            ) {
                if (uiState.isApplying) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SalatOnPrimary)
                } else {
                    Text(stringResource(R.string.bulk_confirm_update), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isApplying) {
                Text(stringResource(R.string.action_cancel), color = SalatOnSecondaryContainer, fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val PrayerType.iconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BulkUpdateScreenPreview() {
    BulkUpdateScreen(
        uiState = BulkUpdateUiState(),
        onDurationChanged   = {},
        onPreviousRange     = {},
        onNextRange         = {},
        onDaySelected       = {},
        onCustomRangeChanged = { _, _ -> },
        onTogglePrayer      = {},
        onSelectAllToggle   = {},
        onStatusChanged     = {},
        onApplyClick        = {},
        onConfirmApply      = {},
        onDismissConfirm    = {},
        onClearApplySuccess = {},
        onErrorDismiss      = {},
    )
}
