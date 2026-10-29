package com.salattracker.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.DayLog
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.domain.usecase.currentPeriodAmount
import com.salattracker.domain.usecase.dayIndex
import com.salattracker.domain.usecase.goalDayState
import com.salattracker.domain.usecase.goalStreak
import com.salattracker.domain.usecase.isDueOn
import com.salattracker.domain.usecase.statusOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class TodaySummary(
    val dailyDone: Int = 0,
    val dailyTotal: Int = 0,
    val periodicDone: Int = 0,
    val periodicTotal: Int = 0,
)

data class GoalCardUi(
    val goal: SpiritualGoal,
    val streak: Int,
    val currentAmount: Int,
    val dueToday: Boolean,
    val skippedToday: Boolean,
    val dayIndex: Int?,
    /** The current Monday–Sunday week. */
    val strip: List<GoalDayUi>,
) {
    val isPeriodDone: Boolean get() = currentAmount >= goal.targetAmount
}

data class ArchivedGoalUi(val goal: SpiritualGoal, val status: GoalStatus)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val today: TodaySummary = TodaySummary(),
    val active: List<GoalCardUi> = emptyList(),
    val archived: List<ArchivedGoalUi> = emptyList(),
    val errorRes: Int? = null,
) {
    val hasAnyGoals: Boolean get() = active.isNotEmpty() || archived.isNotEmpty()
}

private const val WEEK_DAYS = 7

internal fun buildGoalsContent(
    histories: List<GoalHistory>,
    today: LocalDate,
): Triple<TodaySummary, List<GoalCardUi>, List<ArchivedGoalUi>> {
    val active = mutableListOf<GoalCardUi>()
    val archived = mutableListOf<ArchivedGoalUi>()
    val weekStart = today.minusDays(today.dayOfWeek.value - 1L)

    histories.forEach { (goal, checkIns) ->
        val status = goal.statusOn(today)
        if (status != GoalStatus.ACTIVE) {
            archived += ArchivedGoalUi(goal, status)
            return@forEach
        }
        val log = DayLog(checkIns)
        val strip = (0 until WEEK_DAYS).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val amount = log.amountOn(date)
            GoalDayUi(
                date = date,
                state = goalDayState(goal, date, amount, log.isSkipped(date), today),
                amount = amount,
                isToday = date == today,
            )
        }
        active += GoalCardUi(
            goal = goal,
            streak = goalStreak(goal, checkIns, today).current,
            currentAmount = if (goal.frequency == GoalFrequency.DAILY) {
                log.amountOn(today)
            } else {
                currentPeriodAmount(goal, checkIns, today)
            },
            dueToday = goal.isDueOn(today),
            skippedToday = log.isSkipped(today),
            dayIndex = goal.dayIndex(today),
            strip = strip,
        )
    }

    val daily = active.filter {
        it.goal.frequency == GoalFrequency.DAILY && it.dueToday && !it.skippedToday
    }
    val periodic = active.filter { it.goal.frequency != GoalFrequency.DAILY }
    val summary = TodaySummary(
        dailyDone = daily.count { it.isPeriodDone },
        dailyTotal = daily.size,
        periodicDone = periodic.count { it.isPeriodDone },
        periodicTotal = periodic.size,
    )
    return Triple(summary, active, archived.sortedBy { it.status != GoalStatus.COMPLETED })
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    private val refreshTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(goalRepository.observeGoalHistories(), refreshTrigger) { histories, _ ->
                histories to LocalDate.now(clock)
            }.catch {
                _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) }
            }.collect { (histories, today) ->
                val (summary, active, archived) = buildGoalsContent(histories, today)
                _uiState.update {
                    it.copy(isLoading = false, today = summary, active = active, archived = archived)
                }
            }
        }
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    fun onQuickAdd(goalId: Long) {
        val card = _uiState.value.active.firstOrNull { it.goal.id == goalId } ?: return
        if (!card.dueToday || card.skippedToday || card.isPeriodDone) return
        viewModelScope.launch {
            runCatching { goalRepository.checkIn(goalId, 1, LocalDate.now(clock)) }
                .onFailure { _uiState.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }
}
