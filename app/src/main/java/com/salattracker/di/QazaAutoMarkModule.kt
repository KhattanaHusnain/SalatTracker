package com.salattracker.di

import com.salattracker.notification.DataStoreQazaAutoMarkStateStore
import com.salattracker.notification.QazaAutoMarkStateStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QazaAutoMarkModule {

    @Binds
    @Singleton
    abstract fun bindQazaAutoMarkStateStore(
        impl: DataStoreQazaAutoMarkStateStore,
    ): QazaAutoMarkStateStore
}
