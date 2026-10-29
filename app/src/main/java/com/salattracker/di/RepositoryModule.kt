package com.salattracker.di

import com.salattracker.core.locale.LocaleApplierImpl
import com.salattracker.data.location.GeocodingRepositoryImpl
import com.salattracker.data.location.LocationProviderImpl
import com.salattracker.data.prayertimes.AdhanPrayerTimeCalculator
import com.salattracker.data.mosque.MosqueGeofencerImpl
import com.salattracker.data.mosque.MosqueModeStoreImpl
import com.salattracker.data.mosque.RingerControllerImpl
import com.salattracker.data.repository.MosqueRepositoryImpl
import com.salattracker.data.repository.MushafRepositoryImpl
import com.salattracker.data.repository.BackupRepositoryImpl
import com.salattracker.data.repository.GoalRepositoryImpl
import com.salattracker.data.repository.PrayerRepositoryImpl
import com.salattracker.data.repository.QuranContentRepositoryImpl
import com.salattracker.data.repository.QuranUserDataRepositoryImpl
import com.salattracker.data.repository.SettingsRepositoryImpl
import com.salattracker.data.local.preferences.DataStoreQazaPreferencesRepository
import com.salattracker.data.repository.TasbihRepositoryImpl
import com.salattracker.data.repository.VerseRepositoryImpl
import com.salattracker.domain.repository.BackupRepository
import com.salattracker.domain.repository.GeocodingRepository
import com.salattracker.domain.repository.GoalReminderScheduler
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.domain.repository.LocaleApplier
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.MosqueGeofencer
import com.salattracker.domain.repository.MosqueModeNotifier
import com.salattracker.domain.repository.MosqueModeStore
import com.salattracker.domain.repository.MosqueModeSyncScheduler
import com.salattracker.domain.repository.MosqueRepository
import com.salattracker.domain.repository.MushafRepository
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.ReminderNotifications
import com.salattracker.domain.repository.PrayerTimeCalculator
import com.salattracker.domain.repository.RingerController
import com.salattracker.notification.MosqueModeNotifierImpl
import com.salattracker.notification.MosqueModeSyncSchedulerImpl
import com.salattracker.notification.GoalReminderSchedulerImpl
import com.salattracker.notification.PrayerReminderReschedulerImpl
import com.salattracker.notification.ReminderNotificationsImpl
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.QazaPreferencesRepository
import com.salattracker.domain.repository.TasbihRepository
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
    abstract fun bindQazaPreferencesRepository(impl: DataStoreQazaPreferencesRepository): QazaPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(impl: PrayerRepositoryImpl): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindTasbihRepository(impl: TasbihRepositoryImpl): TasbihRepository

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
    abstract fun bindGeocodingRepository(impl: GeocodingRepositoryImpl): GeocodingRepository

    @Binds
    @Singleton
    abstract fun bindPrayerTimeCalculator(impl: AdhanPrayerTimeCalculator): PrayerTimeCalculator

    @Binds
    @Singleton
    abstract fun bindPrayerReminderRescheduler(impl: PrayerReminderReschedulerImpl): PrayerReminderRescheduler

    @Binds
    @Singleton
    abstract fun bindReminderNotifications(impl: ReminderNotificationsImpl): ReminderNotifications

    @Binds
    @Singleton
    abstract fun bindLocaleApplier(impl: LocaleApplierImpl): LocaleApplier

    @Binds
    @Singleton
    abstract fun bindMosqueRepository(impl: MosqueRepositoryImpl): MosqueRepository

    @Binds
    @Singleton
    abstract fun bindMosqueModeStore(impl: MosqueModeStoreImpl): MosqueModeStore

    @Binds
    @Singleton
    abstract fun bindMosqueGeofencer(impl: MosqueGeofencerImpl): MosqueGeofencer

    @Binds
    @Singleton
    abstract fun bindRingerController(impl: RingerControllerImpl): RingerController

    @Binds
    @Singleton
    abstract fun bindMosqueModeNotifier(impl: MosqueModeNotifierImpl): MosqueModeNotifier

    @Binds
    @Singleton
    abstract fun bindMosqueModeSyncScheduler(impl: MosqueModeSyncSchedulerImpl): MosqueModeSyncScheduler

    @Binds
    @Singleton
    abstract fun bindQuranUserDataRepository(impl: QuranUserDataRepositoryImpl): QuranUserDataRepository

    @Binds
    @Singleton
    abstract fun bindQuranContentRepository(impl: QuranContentRepositoryImpl): QuranContentRepository

    @Binds
    @Singleton
    abstract fun bindMushafRepository(impl: MushafRepositoryImpl): MushafRepository
}
