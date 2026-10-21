package com.salattracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.salattracker.data.local.dao.*
import com.salattracker.data.local.entity.*

@Database(
    entities = [
        PrayerEntryEntity::class,
        StreakSnapshotEntity::class,
        QazaBalanceEntity::class,
        VerseCacheEntity::class,
        SpiritualGoalEntity::class,
        GoalCheckInEntity::class,
        MosqueEntity::class,
    ],
    version  = 8,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SalatDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun streakDao(): StreakDao
    abstract fun verseDao(): VerseDao
    abstract fun goalDao(): GoalDao
    abstract fun backupDao(): BackupDao
    abstract fun mosqueDao(): MosqueDao
}