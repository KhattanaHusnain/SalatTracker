package com.salattracker.di

import com.salattracker.data.backup.BackupLocalSource
import com.salattracker.data.backup.BackupStateStore
import com.salattracker.data.backup.DataStoreBackupStateStore
import com.salattracker.data.backup.DriveAuthorizer
import com.salattracker.data.backup.DriveBackupStore
import com.salattracker.data.backup.PlayServicesDriveAuthorizer
import com.salattracker.data.backup.RemoteBackupStore
import com.salattracker.data.backup.RoomBackupLocalSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupLocalSource(impl: RoomBackupLocalSource): BackupLocalSource

    @Binds
    @Singleton
    abstract fun bindBackupStateStore(impl: DataStoreBackupStateStore): BackupStateStore

    @Binds
    @Singleton
    abstract fun bindRemoteBackupStore(impl: DriveBackupStore): RemoteBackupStore

    @Binds
    @Singleton
    abstract fun bindDriveAuthorizer(impl: PlayServicesDriveAuthorizer): DriveAuthorizer
}
