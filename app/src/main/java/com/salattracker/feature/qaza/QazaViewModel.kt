package com.salattracker.feature.qaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.usecase.QazaDay
import com.salattracker.domain.usecase.QazaProgress
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.domain.usecase.PACE_WINDOW_DAYS
import com.salattracker.domain.usecase.groupPendingByDay
import com.salattracker.domain.usecase.pendingByPrayer
import com.salattracker.domain.usecase.pickOldestPending
import com.salattracker.domain.usecase.qazaProgress
import com.salattracker.domain.usecase.toQazaSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── UI models ─────────────────────────────────────────────────────────────────

/** One row of the "Recently made up" section. */
data class RecentMadeUpUi(val slot: QazaSlot, val madeUpOn: LocalDate)

data class QazaUiState(
    val isLoading: Boolean = true,
    val progress: QazaProgress = qazaProgress(outstanding = 0, madeUpTotal = 0, madeUpAtMillis = emptyList()),
    val pendingByPrayer: Map<PrayerType, Int> = PrayerType.values().associateWith { 0 },
    /** The newest few days that still have pending prayers — enough to show something without
     * duplicating the full Missed Prayers list. */
    val latestDays: List<QazaDay> = emptyList(),
    val totalPendingDays: Int = 0,
    val recentMadeUp: List<RecentMadeUpUi> = emptyList(),
    val recentExpanded: Boolean = false,
    val logSheet: QuickLogSheetUi? = null,
    val undo: QazaUndoUi? = null,
    val errorRes: Int? = null,
) {
    val hasOutstanding: Boolean get() = progress.outstanding > 0
}

private const val LATEST_DAYS_SHOWN = 3
private const val RECENT_MADE_UP_SHOWN = 5

@HiltViewModel
class QazaViewModel @Inject constructor(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QazaUiState())
    val uiState: StateFlow<QazaUiState> = _uiState.asStateFlow()

    // Pending slots kept around (not just derived UI) so quick-log and undo can act on the
    // real backlog rather than only on what's currently rendered.
    private var pendingSlots: List<QazaSlot> = emptyList()

    private var undoJob: Job? = null

    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeBacklog()
    }

    /** Re-derives "today" — called once when the tab is entered, same pattern as Goals' own
     * refresh(), so a midnight rollover is picked up without an always-running ticker. */
    fun refresh() = refreshTrigger.update { it + 1 }

    private fun observeBacklog() {
        viewModelScope.launch {
            combine(
                repository.observePendingQazaEntries(),
                repository.observeQazaMadeUpCount(),
                repository.observeQazaMadeUpSince(LocalDate.now().minusDays((PACE_WINDOW_DAYS - 1).toLong())),
                repository.observeRecentQazaMadeUp(RECENT_MADE_UP_SHOWN),
                refreshTrigger,
            ) { pending, madeUpCount, madeUpSince, recentMadeUp, _ ->
                Quad(pending, madeUpCount, madeUpSince, recentMadeUp)
            }
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { _ -> _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } }
                .collect { (pending, madeUpCount, madeUpSince, recentMadeUp) ->
                    val slots = pending.map { it.toQazaSlot() }
                    pendingSlots = slots
                    val today = LocalDate.now()
                    val latestDays = groupPendingByDay(slots, newestFirst = true)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            progress = qazaProgress(
                                outstanding = slots.size,
                                madeUpTotal = madeUpCount,
                                madeUpAtMillis = madeUpSince.map { it.updatedAt },
                                today = today,
                            ),
                            pendingByPrayer = pendingByPrayer(slots),
                            latestDays = latestDays.take(LATEST_DAYS_SHOWN),
                            totalPendingDays = latestDays.size,
                            recentMadeUp = recentMadeUp.map { RecentMadeUpUi(it.toQazaSlot(), it.date) },
                        )
                    }
                }
        }
    }

    // ── Single / day make-up ──────────────────────────────────────────────────

    fun onMakeUp(slot: QazaSlot) = makeUp(listOf(slot))

    fun onMakeUpDay(day: QazaDay) = makeUp(day.prayers.map { QazaSlot(day.date, it) })

    private fun makeUp(slots: List<QazaSlot>) {
        if (slots.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.bulkUpsertPrayerStatus(slots.map { it.date to it.prayerType }, PrayerStatus.QAZA_COMPLETED)
            }.onSuccess {
                showUndo(slots)
            }.onFailure {
                _uiState.update { it.copy(errorRes = R.string.error_save_generic) }
            }
        }
    }

    /** Reverts the slots behind the current undo snackbar, then dismisses it. A no-op once the
     * window has already expired (undo is already null). */
    fun onUndo() {
        val slots = pendingUndoSlots ?: return
        pendingUndoSlots = null
        undoJob?.cancel()
        _uiState.update { it.copy(undo = null) }
        viewModelScope.launch {
            runCatching {
                repository.bulkUpsertPrayerStatus(slots.map { it.date to it.prayerType }, PrayerStatus.QAZA_PENDING)
            }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    /** Reverts one row of "Recently made up" directly — no snackbar, since the row itself stays
     * on screen (collapsed back into the backlog) as the confirmation. */
    fun onRecentUndo(entry: RecentMadeUpUi) {
        viewModelScope.launch {
            runCatching {
                repository.upsertPrayerStatus(entry.slot.date, entry.slot.prayerType, PrayerStatus.QAZA_PENDING)
            }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    private var pendingUndoSlots: List<QazaSlot>? = null

    private fun showUndo(slots: List<QazaSlot>) {
        pendingUndoSlots = slots
        undoJob?.cancel()
        _uiState.update { it.copy(undo = QazaUndoUi(count = slots.size)) }
        undoJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MILLIS)
            pendingUndoSlots = null
            _uiState.update { it.copy(undo = null) }
        }
    }

    fun onUndoDismiss() {
        undoJob?.cancel()
        pendingUndoSlots = null
        _uiState.update { it.copy(undo = null) }
    }

    // ── Recently made up section ──────────────────────────────────────────────

    fun onRecentExpandToggle() = _uiState.update { it.copy(recentExpanded = !it.recentExpanded) }

    // ── Quick-log sheet ────────────────────────────────────────────────────────

    fun onLogClick() {
        val outstanding = pendingByPrayer(pendingSlots)
        _uiState.update {
            it.copy(logSheet = QuickLogSheetUi(PrayerType.values().map { type -> QuickLogRowUi(type, outstanding[type] ?: 0, 0) }))
        }
    }

    fun onLogCountChanged(prayerType: PrayerType, count: Int) {
        _uiState.update { state ->
            val sheet = state.logSheet ?: return@update state
            val rows = sheet.rows.map { row ->
                if (row.prayerType == prayerType) row.copy(count = count.coerceIn(0, row.outstanding)) else row
            }
            state.copy(logSheet = sheet.copy(rows = rows))
        }
    }

    fun onLogDismiss() = _uiState.update { it.copy(logSheet = null) }

    fun onLogConfirm() {
        val sheet = _uiState.value.logSheet ?: return
        val counts = sheet.rows.filter { it.count > 0 }.associate { it.prayerType to it.count }
        if (counts.isEmpty()) return
        val slots = pickOldestPending(pendingSlots, counts)
        _uiState.update { it.copy(logSheet = null) }
        makeUp(slots)
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

    companion object {
        private const val UNDO_WINDOW_MILLIS = 6_000L
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
