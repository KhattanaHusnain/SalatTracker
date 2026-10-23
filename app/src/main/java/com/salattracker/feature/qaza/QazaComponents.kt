package com.salattracker.feature.qaza

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.label
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.QazaDay
import com.salattracker.domain.usecase.QazaMonth
import com.salattracker.domain.usecase.toHijriOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Shared building blocks for the Qaza dashboard and the full Missed Prayers list ──────────

/** How one pill in a [QazaDayCard] (or the quick-log sheet's rows) should be drawn. */
enum class QazaPillState { PENDING, SELECTED, MADE_UP }

/** A quick-log row for one prayer: how many are outstanding, and how many the person has
 * chosen to mark made up (0..outstanding). */
data class QuickLogRowUi(val prayerType: PrayerType, val outstanding: Int, val count: Int)

data class QuickLogSheetUi(val rows: List<QuickLogRowUi>) {
    val totalCount: Int get() = rows.sumOf { it.count }
}

/** The undo window shown as a snackbar after a make-up action — [slots] is exactly what would
 * be reverted back to pending. */
data class QazaUndoUi(val count: Int)

@Composable
internal fun rememberQazaDateFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
}

@Composable
internal fun rememberQazaMonthFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
}

/** "22 Rajab" — day + Hijri month name, no year (the year rarely adds anything for a backlog
 * that's at most a couple of years deep). Null when the platform can't convert this date. */
@Composable
internal fun qazaHijriDayLabel(date: LocalDate): String? {
    val hijri = date.toHijriOrNull() ?: return null
    val months = stringArrayResource(R.array.hijri_months)
    return "${hijri.day} ${months[hijri.month - 1]}"
}

// ── Prayer pill ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QazaPrayerPill(
    prayerType: PrayerType,
    state: QazaPillState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val (bg, fg) = when (state) {
        QazaPillState.PENDING  -> QazaPendingPillBg to QazaPendingPillFg
        QazaPillState.SELECTED -> SalatPrimaryContainer to SalatOnPrimaryContainer
        QazaPillState.MADE_UP  -> QazaMadeUpPillBg to QazaMadeUpPillFg
    }
    val label = prayerType.label()
    val pillDescription = qazaPillContentDescription(label, state)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = pillDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state == QazaPillState.SELECTED) {
            Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun qazaPillContentDescription(label: String, state: QazaPillState): String = when (state) {
    QazaPillState.PENDING  -> stringResource(R.string.qaza_cd_pill_pending, label)
    QazaPillState.SELECTED -> stringResource(R.string.qaza_cd_pill_selected, label)
    QazaPillState.MADE_UP  -> stringResource(R.string.qaza_cd_pill_made_up, label)
}

// ── Day card ──────────────────────────────────────────────────────────────────

/**
 * One date's missed prayers — a tappable header (opens that day in Calendar) plus a "Make up
 * all" action, with each prayer as a [QazaPrayerPill]. Shared by the Qaza tab's "Latest missed
 * prayers" and the full Missed Prayers list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QazaDayCard(
    day: QazaDay,
    selectionMode: Boolean,
    selectedPrayers: Set<PrayerType>,
    onPillClick: (PrayerType) -> Unit,
    onPillLongClick: (PrayerType) -> Unit,
    onMakeUpAll: () -> Unit,
    onOpenDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = rememberQazaDateFormatter()
    val hijriLabel = qazaHijriDayLabel(day.date)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurface,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
    ) {
        val openDayDescription = stringResource(R.string.qaza_cd_open_day, day.date.format(dateFormatter))
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(onClick = onOpenDay)
                        .semantics { contentDescription = openDayDescription },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(day.date.format(dateFormatter), color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                    if (hijriLabel != null) {
                        Text(hijriLabel, color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!selectionMode) {
                    TextButton(onClick = onMakeUpAll) {
                        Text(stringResource(R.string.qaza_make_up_all), color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                day.prayers.forEach { prayer ->
                    val state = if (selectionMode && prayer in selectedPrayers) QazaPillState.SELECTED else QazaPillState.PENDING
                    QazaPrayerPill(
                        prayerType = prayer,
                        state = state,
                        onClick = { onPillClick(prayer) },
                        onLongClick = { onPillLongClick(prayer) },
                    )
                }
            }
        }
    }
}

// ── Month header (full list) ─────────────────────────────────────────────────

@Composable
fun QazaMonthHeader(month: QazaMonth, modifier: Modifier = Modifier) {
    val formatter = rememberQazaMonthFormatter()
    val count = month.days.sumOf { it.prayers.size }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = month.month.atDay(1).format(formatter),
            color = SalatOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
        )
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(QazaPendingPillBg).padding(horizontal = 8.dp, vertical = 1.dp),
        ) {
            Text("$count", color = QazaPendingPillFg,  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
    }
}

// ── Quick-log bottom sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    state: QuickLogSheetUi,
    onCountChanged: (PrayerType, Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurfaceContainerLowest) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.qaza_log_title), color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.qaza_log_hint), color = SalatOnSurfaceVariant, fontSize = 12.sp,
                fontFamily = AppUiFontFamily, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.rows.forEach { row ->
                    QuickLogRow(row = row, onCountChanged = { count -> onCountChanged(row.prayerType, count) })
                }
            }

            Spacer(Modifier.height(20.dp))
            com.salattracker.core.ui.SalatPrimaryButton(
                onClick = onConfirm,
                enabled = state.totalCount > 0,
            ) {
                Text(stringResource(R.string.qaza_log_confirm, state.totalCount), fontFamily = AppUiFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QuickLogRow(row: QuickLogRowUi, onCountChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(row.prayerType.iconRes), null, tint = SalatSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.prayerType.label(), color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
            Text(stringResource(R.string.qaza_log_outstanding, row.outstanding), color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
        FilledTonalIconButton(
            onClick = { onCountChanged((row.count - 1).coerceAtLeast(0)) },
            enabled = row.count > 0,
            modifier = Modifier.size(30.dp),
        ) { Text("−", fontSize = 16.sp) }
        Text(
            "${row.count}", color = if (row.count > 0) SalatPrimary else SalatOutline, fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onCountChanged((row.count + 1).coerceAtMost(row.outstanding)) },
            enabled = row.count < row.outstanding,
            modifier = Modifier.size(30.dp),
        ) { Text("+", fontSize = 16.sp) }
    }
}

// ── Ad hoc colour pair for the pending/made-up pill fills (not part of the shared status
// palette since no other screen draws a Qaza prayer as a pill) ──────────────────────────────
private val QazaPendingPillBg = Color(0xFFFFDCC3)
private val QazaPendingPillFg = Color(0xFF6E3900)
private val QazaMadeUpPillBg  = Color(0xFFF3E8FF)
private val QazaMadeUpPillFg  = Color(0xFF6B21A8)
