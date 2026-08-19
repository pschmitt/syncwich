package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.db.AppDatabase
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Signs the user out: wipes the stored server URL/API token and the offline recipe cache together,
 * so switching to (or re-onboarding) a different Mealie server never shows stale data cached from
 * the previous one.
 */
@Singleton
class AccountRepository
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase,
    private val recipeHistoryRepository: RecipeHistoryRepository,
) {

    suspend fun signOut() {
        settingsRepository.clear()
        settingsRepository.resetSyncState()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        recipeHistoryRepository.clear()
    }
}
