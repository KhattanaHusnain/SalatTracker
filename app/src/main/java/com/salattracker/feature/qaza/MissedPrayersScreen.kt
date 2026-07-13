package com.salattracker.feature.qaza

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.salattracker.core.ui.label
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate

// ── Missed Prayers Screen ─────────────────────────────────────────────────────

@Composable
fun MissedPrayersScreen(
    uiState: MissedPrayersUiState,
    onFilterSelected: (PrayerType?) -> Unit,
    onToggleSortOrder: () -> Unit,
    onSelectMultipleClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onToggleEntrySelected: (String) -> Unit,
    onMarkCompleted: (QazaPendingEntryUi) -> Unit,
    onCompleteSelected: () -> Unit,
    onBackToDashboard: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(label = stringResource(R.string.filter_all), isSelected = uiState.filter == null, onClick = { onFilterSelected(null) })
                PrayerType.values().forEach { type ->
                    FilterChip(
                        label      = type.label(),
                        isSelected = uiState.filter == type,
                        onClick    = { onFilterSelected(type) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Stats + sort row ──────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history_edu),
                        contentDescription = null,
                        tint = SalatOnSecondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.missed_total_pending, uiState.totalPending),
                        color = SalatOnSecondaryContainer,
                        fontSize = 11.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onToggleSortOrder) {
                        Text(
                            text = stringResource(if (uiState.sortNewestFirst) R.string.sort_newest else R.string.sort_oldest),
                            color = SalatPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
                        )
                    }
                    TextButton(onClick = onSelectMultipleClick) {
                        Text(
                            text = stringResource(if (uiState.isSelectionMode) R.string.action_deselect_all else R.string.action_select_multiple),
                            color = SalatPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Missed prayer list ──────────────────────────────────────────────
            if (!uiState.isLoading && uiState.entries.isEmpty()) {
                MissedPrayersEmptyState(onBackToDashboard = onBackToDashboard)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = if (uiState.isSelectionMode) 120.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        key(entry.id) {
                            val isCompleting = entry.id in uiState.completingIds
                            AnimatedVisibility(
                                visible = !isCompleting,
                                exit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(400),
                                ) + fadeOut(tween(400)),
                            ) {
                                MissedPrayerRow(
                                    entry             = entry,
                                    isSelectionMode   = uiState.isSelectionMode,
                                    isSelected        = entry.id in uiState.selectedIds,
                                    onToggleSelected  = { onToggleEntrySelected(entry.id) },
                                    onMarkCompleted   = { onMarkCompleted(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Bulk action bar ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape    = RoundedCornerShape(16.dp),
                color    = SalatPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.selected_label), color = SalatOnPrimaryContainer, fontSize = 11.sp, fontFamily = InterFamily)
                        Text(
                            text = pluralStringResource(R.plurals.selected_prayers, uiState.selectedIds.size, uiState.selectedIds.size),
                            color = SalatOnPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MontserratFamily,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onCancelSelection,
                            border  = BorderStroke(1.dp, SalatOnPrimary),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = SalatOnPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text(stringResource(R.string.action_cancel), fontSize = 12.sp, fontFamily = InterFamily) }
                        Button(
                            onClick = onCompleteSelected,
                            enabled = uiState.selectedIds.isNotEmpty(),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = SalatSecondaryFixed,
                                contentColor   = SalatPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text(stringResource(R.string.action_complete_all), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily) }
                    }
                }
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.error?.let { message ->
            Snackbar(
                modifier       = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action         = { TextButton(onClick = onErrorDismiss) { Text(stringResource(R.string.action_ok), color = SalatPrimaryFixed) } },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(message, fontFamily = InterFamily, fontSize = 14.sp) }
        }
    }
}

// ── Filter chip ───────────────────────────────────────────────────────────────

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = if (isSelected) SalatPrimaryContainer else SalatSurfaceContainerLow,
        border  = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color      = if (isSelected) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
        )
    }
}

// ── Missed prayer row ─────────────────────────────────────────────────────────

@Composable
private fun MissedPrayerRow(
    entry: QazaPendingEntryUi,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onMarkCompleted: () -> Unit,
) {
    Surface(
        onClick  = { if (isSelectionMode) onToggleSelected() },
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurface,
        border   = BorderStroke(1.dp, if (isSelected) SalatPrimary else SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    colors = CheckboxDefaults.colors(checkedColor = SalatPrimaryContainer),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier         = Modifier.size(48.dp).clip(CircleShape).background(SalatSecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(entry.prayerType.iconRes), null, tint = SalatOnSecondaryContainer, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.prayerType.label(), color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                Text(entry.dateLabel, color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
            }

            if (!isSelectionMode) {
                Button(
                    onClick  = onMarkCompleted,
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_check_circle_filled), null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_mark_done), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                }
            }
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
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.missed_empty_title),
            color = SalatPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFamily,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.missed_empty_desc),
            color = SalatOnSurfaceVariant,
            fontSize = 14.sp,
            fontFamily = InterFamily,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBackToDashboard,
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
        ) {
            Icon(painterResource(R.drawable.ic_mosque), null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_back_dashboard), fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MissedPrayersScreenPreview() {
    MissedPrayersScreen(
        uiState = MissedPrayersUiState(
            isLoading    = false,
            totalPending = 5,
            entries = listOf(
                QazaPendingEntryUi("1", PrayerType.FAJR,    LocalDate.now().minusDays(1), "Monday, 22 Jan"),
                QazaPendingEntryUi("2", PrayerType.DHUHR,   LocalDate.now().minusDays(1), "Monday, 22 Jan"),
                QazaPendingEntryUi("3", PrayerType.ISHA,    LocalDate.now().minusDays(2), "Sunday, 21 Jan"),
                QazaPendingEntryUi("4", PrayerType.MAGHRIB, LocalDate.now().minusDays(2), "Sunday, 21 Jan"),
                QazaPendingEntryUi("5", PrayerType.ASR,     LocalDate.now().minusDays(3), "Saturday, 20 Jan"),
            ),
        ),
        onFilterSelected      = {},
        onToggleSortOrder     = {},
        onSelectMultipleClick = {},
        onCancelSelection     = {},
        onToggleEntrySelected = {},
        onMarkCompleted       = {},
        onCompleteSelected    = {},
        onBackToDashboard     = {},
        onErrorDismiss        = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MissedPrayersScreenEmptyPreview() {
    MissedPrayersScreen(
        uiState = MissedPrayersUiState(isLoading = false, totalPending = 0, entries = emptyList()),
        onFilterSelected      = {},
        onToggleSortOrder     = {},
        onSelectMultipleClick = {},
        onCancelSelection     = {},
        onToggleEntrySelected = {},
        onMarkCompleted       = {},
        onCompleteSelected    = {},
        onBackToDashboard     = {},
        onErrorDismiss        = {},
    )
}
