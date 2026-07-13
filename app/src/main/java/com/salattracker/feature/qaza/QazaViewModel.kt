package com.salattracker.feature.qaza

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                    // The dashboard only surfaces today's Qaza — the "View All" screen covers the full backlog.
                    val today = LocalDate.now()
                    val rows = entries
                        .filter { it.date == today }
                        .map { entry ->
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
        total == 0  -> context.getString(R.string.qaza_insight_none)
        total <= 5  -> context.getString(R.string.qaza_insight_almost, total)
        total <= 15 -> context.getString(R.string.qaza_insight_progress, total)
        else        -> context.getString(R.string.qaza_insight_many)
    }
}