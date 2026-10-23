package com.salattracker.feature.qaza

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.usecase.QazaMonth
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.domain.usecase.groupByMonth
import com.salattracker.domain.usecase.groupPendingByDay
import com.salattracker.domain.usecase.pendingByPrayer
import com.salattracker.domain.usecase.toQazaSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MissedPrayersUiState(
    val isLoading: Boolean = true,
    val filter: PrayerType? = null,                 // null = All
    val countsByPrayer: Map<PrayerType, Int> = PrayerType.values().associateWith { 0 },
    val totalPending: Int = 0,
    val sortNewestFirst: Boolean = true,
    val months: List<QazaMonth> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedSlots: Set<QazaSlot> = emptySet(),
    val undo: QazaUndoUi? = null,
    val errorRes: Int? = null,
)

@HiltViewModel
class MissedPrayersViewModel @Inject constructor(
    private val repository: PrayerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Arriving from a prayer tile on the Qaza tab preselects that prayer's filter; any other
     * entry point (the dashboard's "View all") has no arg and starts on All. */
    private val initialFilter: PrayerType? = savedStateHandle.get<String>("prayer")
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { PrayerType.valueOf(it) }.getOrNull() }

    private var rawPending: List<QazaSlot> = emptyList()
    private var pendingUndoSlots: List<QazaSlot>? = null
    private var undoJob: Job? = null

    private val _uiState = MutableStateFlow(MissedPrayersUiState(filter = initialFilter))
    val uiState: StateFlow<MissedPrayersUiState> = _uiState.asStateFlow()

    init { observeEntries() }

    private fun observeEntries() {
        viewModelScope.launch {
            repository.observePendingQazaEntries()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { _ -> _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } }
                .collect { entries ->
                    rawPending = entries.map { it.toQazaSlot() }
                    _uiState.update { it.copy(isLoading = false, countsByPrayer = pendingByPrayer(rawPending), totalPending = rawPending.size) }
                    regroup()
                }
        }
    }

    private fun regroup() {
        _uiState.update { state ->
            val days = groupPendingByDay(rawPending, prayer = state.filter, newestFirst = state.sortNewestFirst)
            state.copy(months = groupByMonth(days))
        }
    }

    // ── Filter / sort ─────────────────────────────────────────────────────────

    fun onFilterSelected(type: PrayerType?) {
        _uiState.update { it.copy(filter = type) }
        regroup()
    }

    fun onToggleSortOrder() {
        _uiState.update { it.copy(sortNewestFirst = !it.sortNewestFirst) }
        regroup()
    }

    // ── Selection mode ────────────────────────────────────────────────────────

    fun onSelectClick() = _uiState.update { it.copy(isSelectionMode = true) }

    fun onCancelSelection() = _uiState.update { it.copy(isSelectionMode = false, selectedSlots = emptySet()) }

    fun onSelectAll() = _uiState.update { state ->
        val allVisible = state.months.flatMap { month -> month.days.flatMap { day -> day.prayers.map { QazaSlot(day.date, it) } } }
        state.copy(selectedSlots = allVisible.toSet())
    }

    fun onClearSelection() = _uiState.update { it.copy(selectedSlots = emptySet()) }

    fun onPillClick(slot: QazaSlot) {
        val state = _uiState.value
        if (state.isSelectionMode) {
            toggleSelected(slot)
        } else {
            makeUp(listOf(slot))
        }
    }

    /** Long-pressing a pill outside selection mode starts selection with that pill selected. */
    fun onPillLongClick(slot: QazaSlot) {
        if (_uiState.value.isSelectionMode) return
        _uiState.update { it.copy(isSelectionMode = true, selectedSlots = setOf(slot)) }
    }

    private fun toggleSelected(slot: QazaSlot) {
        _uiState.update { state ->
            state.copy(selectedSlots = if (slot in state.selectedSlots) state.selectedSlots - slot else state.selectedSlots + slot)
        }
    }

    fun onMakeUpSelected() {
        val slots = _uiState.value.selectedSlots.toList()
        if (slots.isEmpty()) return
        _uiState.update { it.copy(isSelectionMode = false, selectedSlots = emptySet()) }
        makeUp(slots)
    }

    // ── Day card actions (mirrors the dashboard) ─────────────────────────────

    fun onMakeUpDay(day: com.salattracker.domain.usecase.QazaDay) = makeUp(day.prayers.map { QazaSlot(day.date, it) })

    private fun makeUp(slots: List<QazaSlot>) {
        if (slots.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.bulkUpsertPrayerStatus(slots.map { it.date to it.prayerType }, PrayerStatus.QAZA_COMPLETED)
            }.onSuccess {
                showUndo(slots)
            }.onFailure { _ -> _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

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

    fun onUndoDismiss() {
        undoJob?.cancel()
        pendingUndoSlots = null
        _uiState.update { it.copy(undo = null) }
    }

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

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

    companion object {
        private const val UNDO_WINDOW_MILLIS = 6_000L
    }
}
