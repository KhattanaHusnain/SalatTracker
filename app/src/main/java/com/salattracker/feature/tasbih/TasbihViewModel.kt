package com.salattracker.feature.tasbih

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.TasbihIncrementResult
import com.salattracker.domain.model.TasbihPhrase
import com.salattracker.domain.model.TasbihSession
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.TasbihRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TasbihNotice { data object FreeCompleted : TasbihNotice; data object GoalCompleted : TasbihNotice; data object GoalNotCredited : TasbihNotice }

data class TasbihUiState(
    val isLoading: Boolean = true,
    val session: TasbihSession? = null,
    val goalName: String? = null,
    val notice: TasbihNotice? = null,
)

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val tasbihRepository: TasbihRepository,
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val goalId = (savedStateHandle.get<Long>("goalId") ?: -1L).takeIf { it >= 0L }
    private val _uiState = MutableStateFlow(TasbihUiState())
    val uiState: StateFlow<TasbihUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val goalName = goalId?.let { goalRepository.getGoal(it)?.name }
            _uiState.update { it.copy(goalName = goalName) }
            tasbihRepository.ensureSession(goalId)
            tasbihRepository.observeSession(goalId).collect { session ->
                _uiState.update { it.copy(isLoading = false, session = session) }
            }
        }
    }

    fun increment() = viewModelScope.launch {
        when (val result = tasbihRepository.increment(goalId)) {
            is TasbihIncrementResult.Completed -> _uiState.update {
                it.copy(notice = when {
                    goalId == null -> TasbihNotice.FreeCompleted
                    result.goalCredited -> TasbihNotice.GoalCompleted
                    else -> TasbihNotice.GoalNotCredited
                })
            }
            else -> Unit
        }
    }

    fun undo() = viewModelScope.launch { tasbihRepository.undo(goalId) }
    fun reset() = viewModelScope.launch { tasbihRepository.reset(goalId) }
    fun newRound() = reset()
    fun updateConfiguration(phrase: TasbihPhrase, target: Int) = viewModelScope.launch {
        tasbihRepository.updateConfiguration(goalId, phrase, target)
    }
    fun consumeNotice() = _uiState.update { it.copy(notice = null) }
}
