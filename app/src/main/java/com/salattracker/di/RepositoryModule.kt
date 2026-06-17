package com.salattracker.di

import com.salattracker.data.repository.PrayerRepositoryImpl
import com.salattracker.data.repository.SettingsRepositoryImpl
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
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
}