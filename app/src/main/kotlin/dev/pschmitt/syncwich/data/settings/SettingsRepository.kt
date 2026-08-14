package dev.pschmitt.syncwich.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.syncwichDataStore by preferencesDataStore(name = "syncwich_prefs")

/** The user's Mealie connection. Both fields must be non-blank for the app to be usable. */
data class MealieCredentials(val serverUrl: String, val apiToken: String) {
    val isValid: Boolean
        get() = serverUrl.isNotBlank() && apiToken.isNotBlank()
}

/**
 * The server base URL and long-lived API token, backed by [EncryptedSharedPreferences] (Android
 * Keystore-tied, hence `allowBackup=false` in the manifest - a restored backup couldn't decrypt
 * these anyway). Read reactively by [dev.pschmitt.syncwich.data.api.DynamicBaseUrlInterceptor] and
 * [dev.pschmitt.syncwich.data.api.AuthInterceptor] at request time, so changing the connection
 * never requires rebuilding the Retrofit/OkHttp stack.
 *
 * Plain, non-secret bookkeeping (last sync timestamp/error) lives in DataStore Preferences
 * instead - see AGENTS.md's architecture section for why credentials and prefs are split this way.
 */
// AndroidX Security Crypto currently deprecates this API without providing a replacement for the
// same encrypted SharedPreferences migration path. Keep it until the library offers one; the
// suppression makes this intentional compatibility boundary visible to the compiler.
@Suppress("DEPRECATION")
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            "syncwich_secure_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private val _credentials = MutableStateFlow(loadCredentials())
    val credentials: StateFlow<MealieCredentials> = _credentials.asStateFlow()

    /** True once both a server URL and API token are stored. */
    val isConfigured: Boolean
        get() = _credentials.value.isValid

    /** Epoch millis of the last successful background/foreground sync, or null if never synced. */
    val lastSyncAt: Flow<Long?> = context.syncwichDataStore.data.map { it[KEY_LAST_SYNC_AT] }

    /**
     * Human-readable message from the most recent failed sync, or null if the last one succeeded.
     */
    val lastSyncError: Flow<String?> =
        context.syncwichDataStore.data.map { it[KEY_LAST_SYNC_ERROR] }

    /** Persists a validated connection. Callers should confirm it works before calling this. */
    fun save(serverUrl: String, apiToken: String) {
        val normalizedUrl = serverUrl.trim().trimEnd('/')
        val trimmedToken = apiToken.trim()
        prefs
            .edit()
            .putString(KEY_SERVER_URL, normalizedUrl)
            .putString(KEY_API_TOKEN, trimmedToken)
            .apply()
        _credentials.value = MealieCredentials(normalizedUrl, trimmedToken)
    }

    /**
     * Wipes the stored server URL and API token. Does not touch the Room cache - see
     * [dev.pschmitt.syncwich.data.repository.AccountRepository.signOut] for the full sign-out.
     */
    fun clear() {
        prefs.edit().clear().apply()
        _credentials.value = MealieCredentials("", "")
    }

    suspend fun recordSyncSuccess() {
        context.syncwichDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_AT] = System.currentTimeMillis()
            prefs.remove(KEY_LAST_SYNC_ERROR)
        }
    }

    suspend fun recordSyncFailure(message: String) {
        context.syncwichDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_ERROR] = message.take(MAX_SYNC_ERROR_LENGTH)
        }
    }

    private fun loadCredentials(): MealieCredentials =
        MealieCredentials(
            serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
            apiToken = prefs.getString(KEY_API_TOKEN, "") ?: "",
        )

    private companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_API_TOKEN = "api_token"
        const val MAX_SYNC_ERROR_LENGTH = 500
        val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val KEY_LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
    }
}
