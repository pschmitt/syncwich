package dev.pschmitt.syncwich.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pschmitt.syncwich.sync.InitialSyncDataSource
import dev.pschmitt.syncwich.sync.RepositoryInitialSyncDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InitialSyncModule {

    @Provides
    @Singleton
    fun provideInitialSyncDataSource(
        dataSource: RepositoryInitialSyncDataSource
    ): InitialSyncDataSource = dataSource
}
