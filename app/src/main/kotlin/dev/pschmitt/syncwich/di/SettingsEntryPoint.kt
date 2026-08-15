package dev.pschmitt.syncwich.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.sync.SyncScheduler

/**
 * Lets test code (e.g. `ScreenshotTest`) reach the app's real Hilt-singleton [SettingsRepository]
 * from outside an `@AndroidEntryPoint` class, via `EntryPointAccessors.fromApplication`. Without
 * this, a test constructing its own `SettingsRepository(context)` would write to the same
 * underlying encrypted prefs file but not the in-memory instance `MainActivity` actually reads,
 * since that instance is cached for the process's lifetime once Hilt creates it.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository

    fun syncScheduler(): SyncScheduler
}
