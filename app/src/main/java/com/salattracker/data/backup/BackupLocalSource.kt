package com.salattracker.data.backup

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.salattracker.data.local.dao.BackupDao
import com.salattracker.data.local.dao.StreakDao
import com.salattracker.data.local.db.SalatDatabase
import com.salattracker.data.local.entity.GoalCheckInEntity
import com.salattracker.data.local.entity.PrayerEntryEntity
import com.salattracker.data.local.entity.SpiritualGoalEntity
import com.salattracker.data.local.entity.StreakSnapshotEntity
import com.salattracker.data.repository.LOCAL_USER_ID
import com.salattracker.core.locale.LocaleManager
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.notification.GoalAlarmScheduler
import com.salattracker.notification.GoalReminderReceiver
import com.salattracker.notification.GoalReminderWorker
import com.salattracker.notification.PrayerReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class LocalBackupCounts(
    val prayerEntryCount: Int,
    val oldestPrayerDate: LocalDate?,
)

/**
 * Reads/writes the data that gets backed up, and performs the local side effects a
 * restore requires (cancel stale goal alarms, reschedule workers, apply language).
 * Kept behind an interface so BackupRepositoryImpl's guard/upload logic can be unit
 * tested with a fake, with no Android dependency.
 */
interface BackupLocalSource {
    suspend fun readSnapshot(): BackupSnapshot
    suspend fun localCounts(): LocalBackupCounts
    /** Replaces all local data with [snapshot] and returns the goal ids that existed before. */
    suspend fun applySnapshot(snapshot: BackupSnapshot): List<Long>
}

@Singleton
class RoomBackupLocalSource @Inject constructor(
    private val database: SalatDatabase,
    private val backupDao: BackupDao,
    private val streakDao: StreakDao,
    private val settingsRepository: SettingsRepository,
    private val prayerRepository: PrayerRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
) : BackupLocalSource {

    private val userId get() = LOCAL_USER_ID

    override suspend fun readSnapshot(): BackupSnapshot = database.withTransaction {
        val prayerEntries = backupDao.getAllPrayerEntries(userId).map {
            BackupPrayerEntry(date = it.date, prayerType = it.prayerType, status = it.status, updatedAt = it.updatedAt)
        }
        val goals = backupDao.getAllGoals().map {
            BackupGoal(
                id = it.id, name = it.name, category = it.category, frequency = it.frequency,
                targetAmount = it.targetAmount, unit = it.unit, reminderEnabled = it.reminderEnabled,
                reminderTime = it.reminderTime, startDate = it.startDate, durationDays = it.durationDays,
                isActive = it.isActive,
            )
        }
        val checkIns = backupDao.getAllCheckIns().map {
            BackupCheckIn(goalId = it.goalId, date = it.date, amount = it.amount)
        }
        val highestStreaks = streakDao.getAllSnapshots(userId).associate { it.prayerType to it.highestStreak }
        val settings = settingsRepository.getCurrentPrefs()

        BackupSnapshot(
            prayerEntries  = prayerEntries,
            goals          = goals,
            checkIns       = checkIns,
            highestStreaks = highestStreaks,
            settings       = settings,
        )
    }

    override suspend fun localCounts(): LocalBackupCounts {
        val count = backupDao.countPrayerEntries(userId)
        val oldest = backupDao.oldestPrayerDate(userId)?.let { LocalDate.parse(it) }
        return LocalBackupCounts(prayerEntryCount = count, oldestPrayerDate = oldest)
    }

    override suspend fun applySnapshot(snapshot: BackupSnapshot): List<Long> {
        val previousGoalIds = database.withTransaction {
            val oldIds = backupDao.getAllGoalIds()

            backupDao.clearPrayerEntries(userId)
            backupDao.clearGoals()
            backupDao.clearCheckIns()
            backupDao.clearStreaks(userId)

            backupDao.insertPrayerEntries(
                snapshot.prayerEntries.map {
                    PrayerEntryEntity(userId = userId, date = it.date, prayerType = it.prayerType, status = it.status, updatedAt = it.updatedAt)
                }
            )
            backupDao.insertGoals(
                snapshot.goals.map {
                    SpiritualGoalEntity(
                        id = it.id, name = it.name, category = it.category, frequency = it.frequency,
                        targetAmount = it.targetAmount, unit = it.unit, reminderEnabled = it.reminderEnabled,
                        reminderTime = it.reminderTime, startDate = it.startDate, durationDays = it.durationDays,
                        isActive = it.isActive,
                    )
                }
            )
            backupDao.insertCheckIns(
                snapshot.checkIns.map { GoalCheckInEntity(goalId = it.goalId, date = it.date, amount = it.amount) }
            )
            // Seed highest streak so recalculateStreaks() carries it forward correctly;
            // current streak is always recomputed below, never trusted from the backup.
            snapshot.highestStreaks.forEach { (type, highest) ->
                streakDao.upsertSnapshot(
                    StreakSnapshotEntity(userId = userId, prayerType = type, currentStreak = 0, highestStreak = highest, lastCalculatedAt = 0L)
                )
            }

            prayerRepository.recalculateStreaks()
            oldIds
        }

        // Settings are DataStore, not Room — written after the transaction commits.
        // onboardingCompleted is written last so a crash mid-restore never finishes
        // onboarding over incompletely-applied data.
        val s = snapshot.settings
        settingsRepository.saveSettings(
            useDeviceLocation     = s.useDeviceLocation,
            latitude               = s.latitude,
            longitude              = s.longitude,
            calculationMethodId    = s.calculationMethodId,
            reminderEnabled        = s.reminderEnabled,
            reminderOffsetMinutes  = s.reminderOffsetMinutes,
            use24HourFormat        = s.use24HourFormat,
            language               = s.language,
            themeMode               = s.themeMode,
            dailyTipsEnabled        = s.dailyTipsEnabled,
        )
        settingsRepository.setOnboardingCompleted(true)

        // Cancel alarms/notifications for every goal that existed before the replace —
        // their ids may now belong to a different restored goal, or to nothing at all.
        val scheduler = GoalAlarmScheduler(context)
        val notifications = NotificationManagerCompat.from(context)
        previousGoalIds.forEach { goalId ->
            scheduler.cancelFor(goalId)
            notifications.cancel(GoalReminderReceiver.notificationIdFor(goalId))
        }

        PrayerReminderWorker.enqueueOneTime(workManager)
        GoalReminderWorker.enqueueOneTime(workManager)
        LocaleManager.apply(s.language)

        return previousGoalIds
    }
}
