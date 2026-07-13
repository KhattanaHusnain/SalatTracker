package com.salattracker.di

import android.content.Context
import androidx.room.Room
import com.salattracker.data.local.dao.*
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
            .fallbackToDestructiveMigration()   // replace with proper migrations before release
            .build()

    @Provides fun providePrayerDao(db: SalatDatabase): PrayerDao          = db.prayerDao()
    @Provides fun provideStreakDao(db: SalatDatabase): StreakDao          = db.streakDao()
    @Provides fun providePrayerTimesDao(db: SalatDatabase): PrayerTimesDao= db.prayerTimesDao()
    @Provides fun provideVerseDao(db: SalatDatabase): VerseDao            = db.verseDao()
    @Provides fun provideGoalDao(db: SalatDatabase): GoalDao              = db.goalDao()
}