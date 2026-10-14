package com.salattracker.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.GoalHistory
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.usecase.DayLog
import com.salattracker.domain.usecase.GoalDayState
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
import java.time.LocalDate
import javax.inject.Inject

// ── UI models ────────────────────────────────────────────────────────────────

/** "Done today 3/4" — daily goals due today (excused days left out), plus weekly/monthly goals. */
data class TodaySummary(
    val dailyDone: Int = 0,
    val dailyTotal: Int = 0,
    val periodicDone: Int = 0,
    val periodicTotal: Int = 0,
)

data class GoalCardUi(
    val goal: SpiritualGoal,
    val streak: Int,
    /** Today's amount for daily goals; this week's / month's amount for weekly / monthly goals. */
    val currentAmount: Int,
    val dueToday: Boolean,
    /** Today is excused for this daily goal. */
    val skippedToday: Boolean,
    val dayIndex: Int?,
    /** The last 7 days, oldest first, ending today. */
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
    val logSheet: LogSheetUi? = null,
    val error: String? = null,
) {
    val hasAnyGoals: Boolean get() = active.isNotEmpty() || archived.isNotEmpty()
}

private const val STRIP_DAYS = 7

/** Pure projection of the stored histories onto what the Goals tab shows for [today]. */
internal fun buildGoalsContent(
    histories: List<GoalHistory>,
    today: LocalDate,
): Triple<TodaySummary, List<GoalCardUi>, List<ArchivedGoalUi>> {
    val active = mutableListOf<GoalCardUi>()
    val archived = mutableListOf<ArchivedGoalUi>()

    histories.forEach { (goal, checkIns) ->
        val status = goal.statusOn(today)
        if (status != GoalStatus.ACTIVE) {
            archived += ArchivedGoalUi(goal, status)
            return@forEach
        }
        val log = DayLog(checkIns)
        val strip = (STRIP_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
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
            currentAmount = if (goal.frequency == GoalFrequency.DAILY) log.amountOn(today) else currentPeriodAmount(goal, checkIns, today),
            dueToday = goal.isDueOn(today),
            skippedToday = log.isSkipped(today),
            dayIndex = goal.dayIndex(today),
            strip = strip,
        )
    }

    val daily = active.filter { it.goal.frequency == GoalFrequency.DAILY && it.dueToday && !it.skippedToday }
    val periodic = active.filter { it.goal.frequency != GoalFrequency.DAILY }
    val summary = TodaySummary(
        dailyDone = daily.count { it.isPeriodDone },
        dailyTotal = daily.size,
        periodicDone = periodic.count { it.isPeriodDone },
        periodicTotal = periodic.size,
    )
    // Completed goals first, then manually archived ones; stable otherwise.
    return Triple(summary, active, archived.sortedBy { it.status != GoalStatus.COMPLETED })
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private var histories: List<GoalHistory> = emptyList()

    /** Bumped by [refresh] so "today" and the strip are recomputed when the screen is shown again
     * (e.g. after midnight) even if no check-in changed. */
    private val refreshTrigger = MutableStateFlow(0)

    init { observeGoals() }

    private fun observeGoals() {
        viewModelScope.launch {
            combine(goalRepository.observeGoalHistories(), refreshTrigger) { h, _ -> h to LocalDate.now() }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (h, today) ->
                    histories = h
                    val (summary, active, archived) = buildGoalsContent(h, today)
                    _uiState.update {
                        it.copy(isLoading = false, today = summary, active = active, archived = archived)
                    }
                }
        }
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    // ── Strip taps ───────────────────────────────────────────────────────────

    fun onDayTap(goalId: Long, date: LocalDate) {
        val card = _uiState.value.active.firstOrNull { it.goal.id == goalId } ?: return
        val day = card.strip.firstOrNull { it.date == date } ?: return
        when (day.state) {
            GoalDayState.LOCKED, GoalDayState.NOT_DUE -> Unit
            // Excused or already-complete multi-step days: tapping can't "add", so open the editor.
            GoalDayState.SKIPPED -> openSheet(card.goal, date)
            GoalDayState.DONE -> if (card.goal.targetAmount > 1) openSheet(card.goal, date) else save(goalId, date, 0, false)
            else -> {
                if (card.goal.targetAmount == 1 && day.amount > 0) {
                    save(goalId, date, 0, false)           // single-step goals toggle off
                } else {
                    viewModelScope.launch {
                        runCatching { goalRepository.checkIn(goalId, 1, date) }
                            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
                    }
                }
            }
        }
    }

    fun onDayLongPress(goalId: Long, date: LocalDate) {
        val card = _uiState.value.active.firstOrNull { it.goal.id == goalId } ?: return
        val day = card.strip.firstOrNull { it.date == date } ?: return
        if (day.state != GoalDayState.LOCKED) openSheet(card.goal, date)
    }

    private fun openSheet(goal: SpiritualGoal, date: LocalDate) {
        val checkIns = histories.firstOrNull { it.goal.id == goal.id }?.checkIns.orEmpty()
        _uiState.update { it.copy(logSheet = goal.logSheetFor(date, checkIns)) }
    }

    // ── Log sheet ────────────────────────────────────────────────────────────

    fun onLogAmountChanged(amount: Int) = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(amount = amount.coerceIn(0, MAX_LOG_AMOUNT)))
    }

    fun onLogSkippedChanged(skipped: Boolean) = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(skipped = skipped))
    }

    fun onLogMarkDone() = _uiState.update { s ->
        s.copy(logSheet = s.logSheet?.copy(amount = s.logSheet.target, skipped = false))
    }

    fun onLogSave() {
        val sheet = _uiState.value.logSheet ?: return
        _uiState.update { it.copy(logSheet = null) }
        save(sheet.goalId, sheet.date, sheet.amount, sheet.skipped)
    }

    fun onLogClear() {
        val sheet = _uiState.value.logSheet ?: return
        _uiState.update { it.copy(logSheet = null) }
        save(sheet.goalId, sheet.date, 0, false)
    }

    fun onLogDismiss() = _uiState.update { it.copy(logSheet = null) }

    private fun save(goalId: Long, date: LocalDate, amount: Int, skipped: Boolean) {
        viewModelScope.launch {
            runCatching { goalRepository.setDayLog(goalId, date, amount, skipped) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
