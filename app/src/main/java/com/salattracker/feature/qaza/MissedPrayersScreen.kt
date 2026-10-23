package com.salattracker.feature.qaza

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.label
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.QazaDay
import com.salattracker.domain.usecase.QazaMonth
import com.salattracker.domain.usecase.QazaSlot

// ── Missed Prayers screen ("View all") ────────────────────────────────────────

@Composable
fun MissedPrayersScreen(
    uiState: MissedPrayersUiState,
    onFilterSelected: (PrayerType?) -> Unit,
    onToggleSortOrder: () -> Unit,
    onSelectClick: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onPillClick: (QazaSlot) -> Unit,
    onPillLongClick: (QazaSlot) -> Unit,
    onMakeUpDay: (QazaDay) -> Unit,
    onMakeUpSelected: () -> Unit,
    onOpenDay: (java.time.LocalDate) -> Unit,
    onUndo: () -> Unit,
    onUndoDismiss: () -> Unit,
    onBackToDashboard: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))

            // ── Filter chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(label = stringResource(R.string.filter_all), count = uiState.totalPending, isSelected = uiState.filter == null, onClick = { onFilterSelected(null) })
                PrayerType.values().forEach { type ->
                    FilterChip(
                        label = type.label(), count = uiState.countsByPrayer[type] ?: 0,
                        isSelected = uiState.filter == type, onClick = { onFilterSelected(type) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Sort + select row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onToggleSortOrder) {
                        Text(
                            text = stringResource(if (uiState.sortNewestFirst) R.string.sort_newest else R.string.sort_oldest),
                            color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
                        )
                    }
                    if (!uiState.isSelectionMode) {
                        TextButton(onClick = onSelectClick) {
                            Text(stringResource(R.string.qaza_select), color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Month-grouped day cards ──────────────────────────────────────
            if (!uiState.isLoading && uiState.months.isEmpty()) {
                MissedPrayersEmptyState(onBackToDashboard = onBackToDashboard)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = if (uiState.isSelectionMode) 120.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.months.forEach { month ->
                        item(key = "header_${month.month}") { QazaMonthHeader(month) }
                        items(month.days, key = { "${month.month}_${it.date}" }) { day ->
                            QazaDayCard(
                                day = day,
                                selectionMode = uiState.isSelectionMode,
                                selectedPrayers = day.prayers.filter { QazaSlot(day.date, it) in uiState.selectedSlots }.toSet(),
                                onPillClick = { prayer -> onPillClick(QazaSlot(day.date, prayer)) },
                                onPillLongClick = { prayer -> onPillLongClick(QazaSlot(day.date, prayer)) },
                                onMakeUpAll = { onMakeUpDay(day) },
                                onOpenDay = { onOpenDay(day.date) },
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Selection bar ─────────────────────────────────────────────────────
        if (uiState.isSelectionMode) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
                shape    = RoundedCornerShape(16.dp),
                color    = SalatPrimary,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.qaza_selected_count, uiState.selectedSlots.size),
                            color = SalatOnPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = onSelectAll) {
                                Text(stringResource(R.string.action_select_all), color = SalatOnPrimary, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                            }
                            TextButton(onClick = onClearSelection) {
                                Text(stringResource(R.string.qaza_clear_selection), color = SalatOnPrimary, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCancelSelection,
                            border  = BorderStroke(1.dp, SalatOnPrimary),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = SalatOnPrimary),
                        ) { Text(stringResource(R.string.action_cancel), fontSize = 12.sp, fontFamily = AppUiFontFamily) }
                        Button(
                            onClick = onMakeUpSelected,
                            enabled = uiState.selectedSlots.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors  = ButtonDefaults.buttonColors(containerColor = SalatSecondaryFixed, contentColor = SalatPrimary),
                        ) { Text(stringResource(R.string.qaza_log_confirm, uiState.selectedSlots.size), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily) }
                    }
                }
            }
        }

        // ── Undo snackbar ─────────────────────────────────────────────────────
        uiState.undo?.let { undo ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onUndo) { Text(stringResource(R.string.qaza_undo), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) {
                Text(
                    text = if (undo.count == 1) stringResource(R.string.qaza_made_up_one) else stringResource(R.string.qaza_made_up_many, undo.count),
                    fontFamily = AppUiFontFamily, fontSize = 14.sp,
                )
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.errorRes?.let { errorRes ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface, contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily, fontSize = 14.sp) }
        }
    }
}

// ── Filter chip ───────────────────────────────────────────────────────────────

@Composable
private fun FilterChip(label: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (isSelected) SalatPrimaryContainer else SalatSurfaceContainerLow,
        border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label, color = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
            )
            Text(
                text = "$count", color = if (isSelected) SalatOnPrimaryContainer else SalatOutline,
                fontSize = 11.sp, fontFamily = AppUiFontFamily,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun MissedPrayersEmptyState(onBackToDashboard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(128.dp).clip(CircleShape).background(SalatSecondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle_filled),
                contentDescription = null,
                tint = SalatPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.missed_empty_title), color = SalatPrimary, fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.missed_empty_desc), color = SalatOnSurfaceVariant, fontSize = 14.sp,
            fontFamily = AppUiFontFamily, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBackToDashboard,
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
        ) {
            Icon(painterResource(R.drawable.ic_mosque), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_back_dashboard), fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MissedPrayersScreenPreview() {
    val today = java.time.LocalDate.now()
    val pending = listOf(
        QazaSlot(today.minusDays(1), PrayerType.FAJR), QazaSlot(today.minusDays(1), PrayerType.DHUHR),
        QazaSlot(today.minusDays(2), PrayerType.ISHA), QazaSlot(today.minusDays(2), PrayerType.MAGHRIB),
        QazaSlot(today.minusDays(3), PrayerType.ASR),
    )
    val days = com.salattracker.domain.usecase.groupPendingByDay(pending)
    MissedPrayersScreen(
        uiState = MissedPrayersUiState(
            isLoading = false,
            countsByPrayer = com.salattracker.domain.usecase.pendingByPrayer(pending),
            totalPending = pending.size,
            months = com.salattracker.domain.usecase.groupByMonth(days),
        ),
        onFilterSelected = {}, onToggleSortOrder = {}, onSelectClick = {}, onSelectAll = {}, onClearSelection = {},
        onCancelSelection = {}, onPillClick = {}, onPillLongClick = {}, onMakeUpDay = {}, onMakeUpSelected = {},
        onOpenDay = {}, onUndo = {}, onUndoDismiss = {}, onBackToDashboard = {}, onErrorDismiss = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MissedPrayersScreenEmptyPreview() {
    MissedPrayersScreen(
        uiState = MissedPrayersUiState(isLoading = false, totalPending = 0, months = emptyList()),
        onFilterSelected = {}, onToggleSortOrder = {}, onSelectClick = {}, onSelectAll = {}, onClearSelection = {},
        onCancelSelection = {}, onPillClick = {}, onPillLongClick = {}, onMakeUpDay = {}, onMakeUpSelected = {},
        onOpenDay = {}, onUndo = {}, onUndoDismiss = {}, onBackToDashboard = {}, onErrorDismiss = {},
    )
}
