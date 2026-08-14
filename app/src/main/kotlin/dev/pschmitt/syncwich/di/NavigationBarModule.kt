package dev.pschmitt.syncwich.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pschmitt.syncwich.data.settings.NavigationBarCacheAvailability
import dev.pschmitt.syncwich.data.settings.NavigationBarPreferences
import dev.pschmitt.syncwich.data.settings.RoomNavigationBarCacheAvailability
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationBarModule {

    @Binds
    @Singleton
    abstract fun bindNavigationBarPreferences(
        settingsRepository: SettingsRepository
    ): NavigationBarPreferences

    @Binds
    @Singleton
    abstract fun bindNavigationBarCacheAvailability(
        cacheAvailability: RoomNavigationBarCacheAvailability
    ): NavigationBarCacheAvailability
}
