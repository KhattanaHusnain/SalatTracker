package com.salattracker.feature.qaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── UI models ─────────────────────────────────────────────────────────────────

data class QazaBalanceUi(
    val prayerType: PrayerType,
    val pendingCount: Int,
)

data class QazaPendingEntryUi(
    val id: String,                     // unique key for list animation
    val prayerType: PrayerType,
    val date: LocalDate,
    val dateLabel: String,              // e.g. "Tuesday, 23 Rajab"
)

data class QazaUiState(
    val isLoading: Boolean = true,
    val totalBacklog: Int = 0,
    val balances: List<QazaBalanceUi> = emptyList(),
    val pendingEntries: List<QazaPendingEntryUi> = emptyList(),
    val insightText: String = "",
    val completingIds: Set<String> = emptySet(),  // IDs currently being animated out
    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class QazaViewModel @Inject constructor(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QazaUiState())
    val uiState: StateFlow<QazaUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")

    init {
        observeQazaData()
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun observeQazaData() {
        viewModelScope.launch {
            repository.observeQazaBalance()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch  { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { balanceMap ->
                    val balances = PrayerType.values().map { type ->
                        QazaBalanceUi(type, balanceMap[type] ?: 0)
                    }
                    val total = balances.sumOf { it.pendingCount }
                    _uiState.update { it.copy(isLoading = false, balances = balances, totalBacklog = total) }
                }
        }

        viewModelScope.launch {
            repository.observePendingQazaEntries()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { entries ->
                    val rows = entries.map { entry ->
                        QazaPendingEntryUi(
                            id          = "${entry.date}_${entry.prayerType}",
                            prayerType  = entry.prayerType,
                            date        = entry.date,
                            dateLabel   = entry.date.format(dateFormatter),
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            pendingEntries = rows,
                            insightText    = buildInsight(state.totalBacklog),
                        )
                    }
                }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Marks one pending Qaza entry as QAZA_COMPLETED.
     * Adds the id to [completingIds] immediately so the UI can animate it out,
     * then writes to Room which triggers the Flow to remove it from the list.
     */
    fun markCompleted(entry: QazaPendingEntryUi) {
        // Optimistically start exit animation
        _uiState.update { it.copy(completingIds = it.completingIds + entry.id) }

        viewModelScope.launch {
            runCatching {
                repository.markQazaCompleted(entry.date, entry.prayerType)
            }.onFailure { e ->
                // Revert animation flag on error
                _uiState.update { state ->
                    state.copy(
                        completingIds = state.completingIds - entry.id,
                        error         = e.message,
                    )
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildInsight(total: Int): String = when {
        total == 0  -> "MashAllah! You have no outstanding Qaza prayers. Keep maintaining your streak!"
        total <= 5  -> "Almost there — only $total prayers remaining. Consistency is key!"
        total <= 15 -> "You've been making progress. At one Qaza per day you'll be clear in $total days."
        else        -> "Breaking it into a few prayers each week makes it manageable. You've got this!"
    }
}