package com.salattracker.data.repository

import androidx.room.withTransaction
import com.salattracker.data.local.dao.GoalDao
import com.salattracker.data.local.dao.TasbihDao
import com.salattracker.data.local.db.SalatDatabase
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.TasbihSessionEntity
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.TasbihIncrementResult
import com.salattracker.domain.model.TasbihPhrase
import com.salattracker.domain.model.TasbihSession
import com.salattracker.domain.repository.TasbihRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasbihRepositoryImpl @Inject constructor(
    private val database: SalatDatabase,
    private val tasbihDao: TasbihDao,
    private val goalDao: GoalDao,
) : TasbihRepository {

    override fun observeSession(goalId: Long?): Flow<TasbihSession?> =
        tasbihDao.observeSession(sessionId(goalId)).map { it?.toDomain() }

    override suspend fun ensureSession(goalId: Long?): TasbihSession = database.withTransaction {
        tasbihDao.getSession(sessionId(goalId))?.toDomain() ?: defaultSession(goalId).also { tasbihDao.upsertSession(it.toEntity()) }
    }

    override suspend fun increment(goalId: Long?): TasbihIncrementResult = database.withTransaction {
        val current = tasbihDao.getSession(sessionId(goalId))?.toDomain() ?: defaultSession(goalId)
        if (current.isCompleted) return@withTransaction TasbihIncrementResult.Ignored
        val nextCount = current.count + 1
        if (current.target == 0 || nextCount < current.target) {
            val next = current.copy(count = nextCount)
            tasbihDao.upsertSession(next.toEntity())
            return@withTransaction TasbihIncrementResult.Updated(next)
        }
        val completed = current.copy(count = current.target, isCompleted = true, completedOn = LocalDate.now())
        val credited = completed.goalId?.let { id ->
            val goal = goalDao.getGoal(id)
            val today = completed.completedOn!!
            if (goal != null && goal.isActive && goal.category == GoalCategory.DHIKR &&
                goal.unit == "Time(s)" && goal.usesTasbihCounter && goal.isDueForTasbih(today)
            ) {
                goalDao.insertCheckIn(GoalCheckInEntity(goalId = id, date = today, amount = 1))
                true
            } else false
        } ?: false
        tasbihDao.upsertSession(completed.toEntity())
        TasbihIncrementResult.Completed(completed, credited)
    }

    override suspend fun undo(goalId: Long?) = database.withTransaction {
        val current = tasbihDao.getSession(sessionId(goalId)) ?: return@withTransaction
        if (!current.isCompleted && current.count > 0) tasbihDao.updateSession(current.copy(count = current.count - 1))
    }

    override suspend fun reset(goalId: Long?) = database.withTransaction {
        val current = tasbihDao.getSession(sessionId(goalId)) ?: return@withTransaction
        tasbihDao.updateSession(current.copy(count = 0, isCompleted = false, completedOn = null))
    }

    override suspend fun updateConfiguration(goalId: Long?, phrase: TasbihPhrase, target: Int) = database.withTransaction {
        val safeTarget = if (goalId == null) target.takeIf { it in setOf(0, 33, 100) } ?: 0 else target.takeIf { it in setOf(33, 100) } ?: 33
        val current = tasbihDao.getSession(sessionId(goalId))?.toDomain() ?: defaultSession(goalId)
        tasbihDao.upsertSession(current.copy(phrase = phrase, target = safeTarget, count = 0, isCompleted = false, completedOn = null).toEntity())
    }

    private fun sessionId(goalId: Long?) = goalId?.let { "goal:$it" } ?: "free"
    private fun defaultSession(goalId: Long?) = TasbihSession(sessionId = sessionId(goalId), goalId = goalId, target = if (goalId == null) 0 else 33)
}

private fun TasbihSessionEntity.toDomain() = TasbihSession(
    sessionId = sessionId, goalId = goalId, phrase = TasbihPhrase.fromId(phraseId), target = target,
    count = count, isCompleted = isCompleted, completedOn = completedOn,
)

private fun TasbihSession.toEntity() = TasbihSessionEntity(
    sessionId = sessionId, goalId = goalId, phraseId = phrase.id, target = target,
    count = count, isCompleted = isCompleted, completedOn = completedOn,
)

private fun com.salattracker.data.local.entity.SpiritualGoalEntity.isDueForTasbih(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    val endDate = if (endsAfterDuration && durationDays > 0) startDate.plusDays(durationDays - 1L) else null
    if (endDate != null && date.isAfter(endDate)) return false
    if (frequency != GoalFrequency.DAILY) return true
    return daysOfWeek == 0 || (daysOfWeek and (1 shl (date.dayOfWeek.value - 1))) != 0
}
