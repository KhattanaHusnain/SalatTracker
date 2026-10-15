package com.salattracker.di

import com.salattracker.core.locale.LocaleApplierImpl
import com.salattracker.data.location.LocationProviderImpl
import com.salattracker.data.repository.BackupRepositoryImpl
import com.salattracker.data.repository.GoalRepositoryImpl
import com.salattracker.data.repository.PrayerRepositoryImpl
import com.salattracker.data.repository.SettingsRepositoryImpl
import com.salattracker.data.repository.VerseRepositoryImpl
import com.salattracker.domain.repository.BackupRepository
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.LocaleApplier
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.notification.GoalReminderSchedulerImpl
import com.salattracker.notification.PrayerReminderReschedulerImpl
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.VerseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(impl: PrayerRepositoryImpl): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindVerseRepository(impl: VerseRepositoryImpl): VerseRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindGoalReminderScheduler(impl: GoalReminderSchedulerImpl): GoalReminderScheduler

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: LocationProviderImpl): LocationProvider

    @Binds
    @Singleton
    abstract fun bindPrayerReminderRescheduler(impl: PrayerReminderReschedulerImpl): PrayerReminderRescheduler

    @Binds
    @Singleton
    abstract fun bindLocaleApplier(impl: LocaleApplierImpl): LocaleApplier
}