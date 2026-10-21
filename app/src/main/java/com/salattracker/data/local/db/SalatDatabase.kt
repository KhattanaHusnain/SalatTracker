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
        SpiritualGoalEntity::class,
        GoalCheckInEntity::class,
        MosqueEntity::class,
        QuranBookmarkEntity::class,
        QuranReadHistoryEntity::class,
        QuranSearchHistoryEntity::class,
        QuranContentPackEntity::class,
    ],
    version  = 11,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SalatDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun streakDao(): StreakDao
    abstract fun goalDao(): GoalDao
    abstract fun backupDao(): BackupDao
    abstract fun mosqueDao(): MosqueDao
    abstract fun quranBookmarkDao(): QuranBookmarkDao
    abstract fun quranReadHistoryDao(): QuranReadHistoryDao
    abstract fun quranSearchHistoryDao(): QuranSearchHistoryDao
    abstract fun quranContentPackDao(): QuranContentPackDao
}
