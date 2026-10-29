package com.salattracker.feature.qaza

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.ShimmerBox
import com.salattracker.core.ui.StatTile
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.shortLabel
import com.salattracker.core.ui.SalatPrimaryButton
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.QazaDay
import com.salattracker.domain.usecase.QazaEtaUnit
import com.salattracker.domain.usecase.QazaSlot
import java.time.LocalDate

// ── Qaza Screen (progress dashboard) ──────────────────────────────────────────

@Composable
fun QazaScreen(
    uiState: QazaUiState,
    onMakeUp: (QazaSlot) -> Unit,
    onMakeUpDay: (QazaDay) -> Unit,
    onUndo: () -> Unit,
    onUndoDismiss: () -> Unit,
    onRecentUndo: (RecentMadeUpUi) -> Unit,
    onRecentExpandToggle: () -> Unit,
    onLogClick: () -> Unit,
    onLogCountChanged: (PrayerType, Int) -> Unit,
    onLogConfirm: () -> Unit,
    onLogDismiss: () -> Unit,
    onViewAllClick: () -> Unit,
    onPrayerClick: (PrayerType) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onAddMissedClick: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            ProgressCard(uiState = uiState, onLogClick = onLogClick, onAddMissedClick = onAddMissedClick)

            Spacer(Modifier.height(16.dp))

            PrayerTilesRow(pendingByPrayer = uiState.pendingByPrayer, isLoading = uiState.isLoading, onPrayerClick = onPrayerClick)

            Spacer(Modifier.height(24.dp))

            LatestMissedSection(
                uiState = uiState,
                onMakeUp = onMakeUp,
                onMakeUpDay = onMakeUpDay,
                onOpenDay = onOpenDay,
                onViewAllClick = onViewAllClick,
            )

            if (uiState.recentMadeUp.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                RecentlyMadeUpSection(
                    entries = uiState.recentMadeUp,
                    expanded = uiState.recentExpanded,
                    onExpandToggle = onRecentExpandToggle,
                    onUndo = onRecentUndo,
                )
            }
        }

        if (uiState.logSheet != null) {
            QuickLogSheet(
                state = uiState.logSheet,
                onCountChanged = onLogCountChanged,
                onConfirm = onLogConfirm,
                onDismiss = onLogDismiss,
            )
        }

        // ── Undo snackbar ─────────────────────────────────────────────────────
        uiState.undo?.let { undo ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = onUndo) {
                        Text(stringResource(R.string.qaza_undo), color = SalatPrimaryFixed)
                    }
                },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
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
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) { Text(stringResource(errorRes), fontFamily = AppUiFontFamily, fontSize = 14.sp) }
        }
    }
}

// ── Progress card ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(uiState: QazaUiState, onLogClick: () -> Unit, onAddMissedClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLowest,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            if (uiState.isLoading) {
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(32.dp))
                Spacer(Modifier.height(16.dp))
                ShimmerBox(Modifier.fillMaxWidth().height(8.dp))
                return@Column
            }

            val progress = uiState.progress
            if (!uiState.hasOutstanding) {
                Text(stringResource(R.string.qaza_all_caught_up), color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.qaza_outstanding), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text("${progress.outstanding}", color = SalatPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
            }

            Spacer(Modifier.height(10.dp))
            val fraction by animateFloatAsState(
                targetValue = (progress.madeUpPercent ?: 100) / 100f,
                animationSpec = tween(durationMillis = 300),
                label = "qazaProgressFraction",
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = SalatStatusQazaCompleted,
                trackColor = SalatSurfaceContainerHigh,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.qaza_made_up_of, progress.madeUpTotal, progress.madeUpTotal + progress.outstanding),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(label = stringResource(R.string.qaza_stat_today), value = "${progress.madeUpToday}", modifier = Modifier.weight(1f))
                StatTile(label = stringResource(R.string.qaza_stat_week), value = "${progress.madeUpThisWeek}", modifier = Modifier.weight(1f))
                val locale = LocalConfiguration.current.locales[0]
                StatTile(label = stringResource(R.string.qaza_stat_per_day), value = String.format(locale, "%.1f", progress.perDay), modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = etaText(progress.eta, progress.hasEnoughSamplesForEta),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )

            if (uiState.hasOutstanding) {
                Spacer(Modifier.height(16.dp))
                SalatPrimaryButton(
                    onClick = onLogClick,
                ) {
                    Text(stringResource(R.string.qaza_action_log), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddMissedClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.qaza_action_add_missed), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
        }
    }
}

@Composable
private fun etaText(eta: com.salattracker.domain.usecase.QazaEta?, hasEnoughSamples: Boolean): String {
    if (!hasEnoughSamples) return stringResource(R.string.qaza_pace_start)
    if (eta == null) return stringResource(R.string.qaza_pace_start)
    return when (eta.unit) {
        QazaEtaUnit.DAYS   -> stringResource(R.string.qaza_eta_days, eta.value)
        QazaEtaUnit.WEEKS  -> stringResource(R.string.qaza_eta_weeks, eta.value)
        QazaEtaUnit.MONTHS -> stringResource(R.string.qaza_eta_months, eta.value)
        QazaEtaUnit.YEARS  -> stringResource(R.string.qaza_eta_years, eta.value)
    }
}

// ── Prayer tiles row ───────────────────────────────────────────────────────────

@Composable
private fun PrayerTilesRow(pendingByPrayer: Map<PrayerType, Int>, isLoading: Boolean, onPrayerClick: (PrayerType) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrayerType.values().forEach { type ->
            val count = pendingByPrayer[type] ?: 0
            Surface(
                onClick = { onPrayerClick(type) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = SalatSurface,
                border = BorderStroke(1.dp, SalatSecondaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isLoading) {
                        ShimmerBox(Modifier.size(18.dp))
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(Modifier.width(24.dp).height(14.dp))
                    } else {
                        val alpha = if (count == 0) 0.4f else 1f
                        Icon(painterResource(type.iconRes), null, tint = SalatSecondary.copy(alpha = alpha), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(type.shortLabel(), color = SalatOnSurfaceVariant.copy(alpha = alpha),  fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count", color = if (count == 0) SalatOutline else SalatPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
                    }
                }
            }
        }
    }
}

// ── Latest missed prayers ─────────────────────────────────────────────────────

@Composable
private fun LatestMissedSection(
    uiState: QazaUiState,
    onMakeUp: (QazaSlot) -> Unit,
    onMakeUpDay: (QazaDay) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.qaza_latest_missed),
            color = SalatOnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
        )
        if (!uiState.isLoading && uiState.totalPendingDays > 0) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = stringResource(R.string.qaza_view_all_count, uiState.progress.outstanding),
                    color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))

    when {
        uiState.isLoading -> {
            repeat(3) { QazaDayCardSkeleton(Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
        }
        uiState.latestDays.isEmpty() -> {
            QazaEmptyState()
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.latestDays.forEach { day ->
                    QazaDayCard(
                        day = day,
                        selectionMode = false,
                        selectedPrayers = emptySet(),
                        onPillClick = { prayer -> onMakeUp(QazaSlot(day.date, prayer)) },
                        onPillLongClick = { },
                        onMakeUpAll = { onMakeUpDay(day) },
                        onOpenDay = { onOpenDay(day.date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QazaDayCardSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier.height(92.dp).clip(RoundedCornerShape(12.dp)))
}

@Composable
private fun QazaEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle_filled),
            contentDescription = null,
            tint = SalatPrimary.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.qaza_all_caught_up),
            color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
        )
    }
}

// ── Recently made up ───────────────────────────────────────────────────────────

@Composable
private fun RecentlyMadeUpSection(
    entries: List<RecentMadeUpUi>,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onUndo: (RecentMadeUpUi) -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "recentMadeUpChevron",
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onExpandToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.qaza_recently_made_up, entries.size),
                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
            )
            Icon(
                painterResource(R.drawable.ic_expand_more), null, tint = SalatOnSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry -> RecentMadeUpRow(entry, onUndo = { onUndo(entry) }) }
            }
        }
    }
}

@Composable
private fun RecentMadeUpRow(entry: RecentMadeUpUi, onUndo: () -> Unit) {
    val dateFormatter = rememberQazaDateFormatter()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SalatSurfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(entry.slot.prayerType.iconRes), null, tint = SalatSecondary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.slot.prayerType.label(), color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
                Text(
                    stringResource(R.string.qaza_made_up_on, entry.madeUpOn.format(dateFormatter)),
                    color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily,
                )
            }
            TextButton(onClick = onUndo) {
                Text(stringResource(R.string.qaza_undo), color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QazaScreenPreview() {
    val pending = listOf(
        QazaSlot(LocalDate.now(), PrayerType.FAJR), QazaSlot(LocalDate.now(), PrayerType.ISHA),
        QazaSlot(LocalDate.now().minusDays(1), PrayerType.DHUHR), QazaSlot(LocalDate.now().minusDays(1), PrayerType.ASR), QazaSlot(LocalDate.now().minusDays(1), PrayerType.MAGHRIB),
    )
    val latestDays = com.salattracker.domain.usecase.groupPendingByDay(pending)
    QazaScreen(
        uiState = QazaUiState(
            isLoading = false,
            progress = com.salattracker.domain.usecase.qazaProgress(outstanding = 58, madeUpTotal = 156, madeUpAtMillis = List(10) { System.currentTimeMillis() }),
            pendingByPrayer = com.salattracker.domain.usecase.pendingByPrayer(pending) + (PrayerType.FAJR to 14),
            latestDays = latestDays,
            totalPendingDays = 12,
            recentMadeUp = listOf(RecentMadeUpUi(QazaSlot(LocalDate.now().minusDays(1), PrayerType.FAJR), LocalDate.now())),
        ),
        onMakeUp = {}, onMakeUpDay = {}, onUndo = {}, onUndoDismiss = {}, onRecentUndo = {}, onRecentExpandToggle = {},
        onLogClick = {}, onLogCountChanged = { _, _ -> }, onLogConfirm = {}, onLogDismiss = {},
        onViewAllClick = {}, onPrayerClick = {}, onOpenDay = {}, onAddMissedClick = {}, onErrorDismiss = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QazaScreenEmptyPreview() {
    QazaScreen(
        uiState = QazaUiState(isLoading = false),
        onMakeUp = {}, onMakeUpDay = {}, onUndo = {}, onUndoDismiss = {}, onRecentUndo = {}, onRecentExpandToggle = {},
        onLogClick = {}, onLogCountChanged = { _, _ -> }, onLogConfirm = {}, onLogDismiss = {},
        onViewAllClick = {}, onPrayerClick = {}, onOpenDay = {}, onAddMissedClick = {}, onErrorDismiss = {},
    )
}
