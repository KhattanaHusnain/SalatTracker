package com.salattracker.feature.qaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MissedPrayersUiState(
    val isLoading: Boolean = true,
    val totalPending: Int = 0,
    val entries: List<QazaPendingEntryUi> = emptyList(),
    val filter: PrayerType? = null,           // null = All
    val sortNewestFirst: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val completingIds: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class MissedPrayersViewModel @Inject constructor(
    private val repository: PrayerRepository,
) : ViewModel() {

    private var rawEntries: List<QazaPendingEntryUi> = emptyList()

    private val _uiState = MutableStateFlow(MissedPrayersUiState())
    val uiState: StateFlow<MissedPrayersUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")

    init { observeEntries() }

    private fun observeEntries() {
        viewModelScope.launch {
            repository.observePendingQazaEntries()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { entries ->
                    rawEntries = entries.map { entry ->
                        QazaPendingEntryUi(
                            id         = "${entry.date}_${entry.prayerType}",
                            prayerType = entry.prayerType,
                            date       = entry.date,
                            dateLabel  = entry.date.format(dateFormatter),
                        )
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    applyFilterAndSort()
                }
        }
    }

    private fun applyFilterAndSort() {
        _uiState.update { state ->
            val filtered = state.filter?.let { f -> rawEntries.filter { it.prayerType == f } } ?: rawEntries
            val sorted = if (state.sortNewestFirst) filtered.sortedByDescending { it.date } else filtered.sortedBy { it.date }
            state.copy(entries = sorted, totalPending = filtered.size)
        }
    }

    fun onFilterSelected(type: PrayerType?) {
        _uiState.update { it.copy(filter = type) }
        applyFilterAndSort()
    }

    fun onToggleSortOrder() {
        _uiState.update { it.copy(sortNewestFirst = !it.sortNewestFirst) }
        applyFilterAndSort()
    }

    /** First tap enters selection mode; subsequent taps toggle select-all/none (mirrors the mockup's button behavior). */
    fun onSelectMultipleClick() {
        val state = _uiState.value
        if (!state.isSelectionMode) {
            _uiState.update { it.copy(isSelectionMode = true) }
        } else {
            val allSelected = state.entries.isNotEmpty() && state.selectedIds.containsAll(state.entries.map { it.id })
            _uiState.update { it.copy(selectedIds = if (allSelected) emptySet() else state.entries.map { it.id }.toSet()) }
        }
    }

    fun onCancelSelection() = _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }

    fun onToggleEntrySelected(id: String) {
        _uiState.update { state ->
            state.copy(selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id)
        }
    }

    fun onMarkCompleted(entry: QazaPendingEntryUi) {
        _uiState.update { it.copy(completingIds = it.completingIds + entry.id) }
        viewModelScope.launch {
            runCatching { repository.markQazaCompleted(entry.date, entry.prayerType) }
                .onFailure { e ->
                    _uiState.update { it.copy(completingIds = it.completingIds - entry.id, error = e.message) }
                }
        }
    }

    fun onCompleteSelected() {
        val state = _uiState.value
        val selected = state.entries.filter { it.id in state.selectedIds }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                repository.bulkUpsertPrayerStatus(selected.map { it.date to it.prayerType }, PrayerStatus.QAZA_COMPLETED)
            }.onSuccess {
                _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
