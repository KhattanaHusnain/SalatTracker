package com.salattracker.di

import android.content.Context
import androidx.room.Room
import com.salattracker.data.local.dao.*
import com.salattracker.data.local.db.MIGRATION_5_6
import com.salattracker.data.local.db.MIGRATION_6_7
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
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()   // only for schemas older than v5 (dev builds)
            .build()

    @Provides fun providePrayerDao(db: SalatDatabase): PrayerDao          = db.prayerDao()
    @Provides fun provideStreakDao(db: SalatDatabase): StreakDao          = db.streakDao()
    @Provides fun providePrayerTimesDao(db: SalatDatabase): PrayerTimesDao= db.prayerTimesDao()
    @Provides fun provideVerseDao(db: SalatDatabase): VerseDao            = db.verseDao()
    @Provides fun provideGoalDao(db: SalatDatabase): GoalDao              = db.goalDao()
    @Provides fun provideBackupDao(db: SalatDatabase): BackupDao          = db.backupDao()
    @Provides fun provideMosqueDao(db: SalatDatabase): MosqueDao          = db.mosqueDao()
}