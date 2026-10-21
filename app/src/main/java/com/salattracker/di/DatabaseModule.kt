package com.salattracker.di

import android.content.Context
import androidx.room.Room
import com.salattracker.data.local.dao.*
import com.salattracker.data.local.db.MIGRATION_5_6
import com.salattracker.data.local.db.MIGRATION_6_7
import com.salattracker.data.local.db.MIGRATION_7_8
import com.salattracker.data.local.db.MIGRATION_8_9
import com.salattracker.data.local.db.MIGRATION_9_10
import com.salattracker.data.local.db.SalatDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSalatDatabase(@ApplicationContext context: Context): SalatDatabase =
        Room.databaseBuilder(
            context,
            SalatDatabase::class.java,
            "salat_tracker.db",
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            // Applies only when NO registered migration covers the device's current version —
            // i.e. a schema older than the oldest migration above, not "any missing path".
            // Every version bump from here on needs its own Migration, or upgrading users lose
            // all their prayer history (there are real Play Store installs on v7 already).
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePrayerDao(db: SalatDatabase): PrayerDao          = db.prayerDao()
    @Provides fun provideStreakDao(db: SalatDatabase): StreakDao          = db.streakDao()
    @Provides fun provideGoalDao(db: SalatDatabase): GoalDao              = db.goalDao()
    @Provides fun provideBackupDao(db: SalatDatabase): BackupDao          = db.backupDao()
    @Provides fun provideMosqueDao(db: SalatDatabase): MosqueDao          = db.mosqueDao()
    @Provides fun provideQuranBookmarkDao(db: SalatDatabase): QuranBookmarkDao         = db.quranBookmarkDao()
    @Provides fun provideQuranReadHistoryDao(db: SalatDatabase): QuranReadHistoryDao   = db.quranReadHistoryDao()
    @Provides fun provideQuranSearchHistoryDao(db: SalatDatabase): QuranSearchHistoryDao = db.quranSearchHistoryDao()
    @Provides fun provideQuranContentPackDao(db: SalatDatabase): QuranContentPackDao   = db.quranContentPackDao()
}