package com.salattracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.core.ui.PrayerStatusSheetArgs
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.usecase.CalculateStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayDate: LocalDate = LocalDate.now(),
    val hijriDate: String = "",
    val prayers: List<PrayerEntryUi> = emptyList(),
    val streak: StreakInfo = StreakInfo(0, 0),
    val sheetArgs: PrayerStatusSheetArgs? = null,   // null = sheet hidden
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class PrayerEntryUi(
    val type: PrayerType,
    val time: LocalTime?,
    val status: PrayerStatus,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayPrayers()
        loadStreak()
    }

    private fun loadTodayPrayers() {
        viewModelScope.launch {
            val today = LocalDate.now()
            prayerRepository.observeTodayPrayers(today)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch   { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { entries ->
                    val times = prayerRepository.getCachedPrayerTimes(today)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            todayDate = today,
                            prayers = PrayerType.values().map { type ->
                                PrayerEntryUi(
                                    type   = type,
                                    time   = times?.get(type),
                                    status = entries.find { it.prayerType == type }?.status
                                        ?: PrayerStatus.NOT_RECORDED,
                                )
                            },
                        )
                    }
                }
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            _uiState.update { it.copy(streak = calculateStreakUseCase()) }
        }
    }

    // ── Sheet ─────────────────────────────────────────────────────────────────

    fun onPrayerCardClick(type: PrayerType) {
        val current = _uiState.value.prayers.find { it.type == type }?.status
            ?: PrayerStatus.NOT_RECORDED
        _uiState.update {
            it.copy(
                sheetArgs = PrayerStatusSheetArgs(
                    prayerType    = type,
                    date          = it.todayDate,
                    currentStatus = current,
                )
            )
        }
    }

    fun onSheetDismiss() = _uiState.update { it.copy(sheetArgs = null) }

    fun onStatusSave(newStatus: PrayerStatus) {
        val args = _uiState.value.sheetArgs ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                prayerRepository.upsertPrayerStatus(args.date, args.prayerType, newStatus)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, sheetArgs = null) }
                loadStreak()
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}