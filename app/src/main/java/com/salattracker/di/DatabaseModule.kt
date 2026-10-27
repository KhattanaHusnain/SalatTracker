package com.salattracker.di

import android.content.Context
import androidx.room.Room
import com.salattracker.data.local.dao.*
import com.salattracker.data.local.db.MIGRATION_5_6
import com.salattracker.data.local.db.MIGRATION_6_7
import com.salattracker.data.local.db.MIGRATION_7_8
import com.salattracker.data.local.db.MIGRATION_8_9
import com.salattracker.data.local.db.MIGRATION_9_10
import com.salattracker.data.local.db.MIGRATION_10_11
import com.salattracker.data.local.db.MIGRATION_11_12
import com.salattracker.data.local.db.MIGRATION_12_13
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
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
            // Historical installs below v5 predate the supported migration chain. From v5 on,
            // missing migrations must fail loudly rather than silently deleting prayer history.
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
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
    @Provides fun provideTasbihDao(db: SalatDatabase): TasbihDao                        = db.tasbihDao()
}
