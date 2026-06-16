package com.salattracker.di

import com.salattracker.data.repository.InMemoryPrayerRepository
import com.salattracker.domain.repository.PrayerRepository
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
    abstract fun bindPrayerRepository(
        impl: InMemoryPrayerRepository,
    ): PrayerRepository
}