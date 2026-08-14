package dev.pschmitt.syncwich.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) :
    NavigationBarPreferences {

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

    /** Preferences used for background work; manual refreshes remain unrestricted. */
    val syncPreferences: Flow<SyncPreferences> =
        context.syncwichDataStore.data.map { prefs ->
            SyncPreferences(
                syncOnlyOnWifi = prefs[KEY_SYNC_ONLY_ON_WIFI] ?: false,
                syncWhileRoaming = prefs[KEY_SYNC_WHILE_ROAMING] ?: true,
                syncIntervalHours =
                    sanitizeSyncIntervalHours(
                        prefs[KEY_SYNC_INTERVAL_HOURS] ?: DEFAULT_SYNC_INTERVAL_HOURS
                    ),
            )
        }

    val syncOnlyOnWifi: Flow<Boolean> = syncPreferences.map { it.syncOnlyOnWifi }
    val syncWhileRoaming: Flow<Boolean> = syncPreferences.map { it.syncWhileRoaming }
    val syncIntervalHours: Flow<Int> = syncPreferences.map { it.syncIntervalHours }

    /** The user's preferred order, with unknown or missing keys resolved by the caller. */
    override val navigationBarOrder: Flow<List<String>> =
        context.syncwichDataStore.data.map { navigationBarOrderFromString(it[KEY_NAV_BAR_ORDER]) }

    /** Destination keys hidden from the bottom navigation bar. */
    override val navigationBarHiddenItems: Flow<Set<String>> =
        context.syncwichDataStore.data.map {
            navigationBarOrderFromString(it[KEY_NAV_BAR_HIDDEN_ITEMS]).toSet()
        }

    /** The user's preferred app text scale, defaulting to the current Material typography size. */
    val fontScale: Flow<Float> =
        context.syncwichDataStore.data.map {
            sanitizeFontScale(it[KEY_FONT_SCALE] ?: DEFAULT_FONT_SCALE)
        }

    val themeMode: Flow<ThemeMode> =
        context.syncwichDataStore.data.map { ThemeMode.fromStorage(it[KEY_THEME_MODE]) }

    val ingredientChecklistEnabled: Flow<Boolean> =
        context.syncwichDataStore.data.map { it[KEY_INGREDIENT_CHECKLIST] ?: false }

    /** Destination keys explicitly shown by the user, even when their cache is empty. */
    override val navigationBarShownItems: Flow<Set<String>> =
        context.syncwichDataStore.data.map {
            navigationBarOrderFromString(it[KEY_NAV_BAR_SHOWN_ITEMS]).toSet()
        }

    /** True after the first complete foreground sync has populated the offline cache. */
    val initialSyncCompleted: Flow<Boolean> =
        context.syncwichDataStore.data.map { it[KEY_INITIAL_SYNC_COMPLETED] ?: false }

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

    suspend fun setSyncOnlyOnWifi(enabled: Boolean) {
        context.syncwichDataStore.edit { prefs -> prefs[KEY_SYNC_ONLY_ON_WIFI] = enabled }
    }

    suspend fun setSyncWhileRoaming(enabled: Boolean) {
        context.syncwichDataStore.edit { prefs -> prefs[KEY_SYNC_WHILE_ROAMING] = enabled }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        context.syncwichDataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL_HOURS] = sanitizeSyncIntervalHours(hours)
        }
    }

    suspend fun saveNavigationBarOrder(order: List<String>) {
        context.syncwichDataStore.edit { prefs ->
            prefs[KEY_NAV_BAR_ORDER] = navigationBarOrderToString(order)
        }
    }

    suspend fun setNavigationBarItemHidden(key: String, hidden: Boolean) {
        context.syncwichDataStore.edit { prefs ->
            val current =
                navigationBarOrderFromString(prefs[KEY_NAV_BAR_HIDDEN_ITEMS]).toMutableSet()
            val explicitlyShown =
                navigationBarOrderFromString(prefs[KEY_NAV_BAR_SHOWN_ITEMS]).toMutableSet()
            if (hidden) {
                current += key
                explicitlyShown -= key
            } else {
                current -= key
                explicitlyShown += key
            }
            prefs[KEY_NAV_BAR_HIDDEN_ITEMS] = navigationBarOrderToString(current.toList())
            prefs[KEY_NAV_BAR_SHOWN_ITEMS] = navigationBarOrderToString(explicitlyShown.toList())
        }
    }

    suspend fun saveFontScale(scale: Float) {
        context.syncwichDataStore.edit { prefs -> prefs[KEY_FONT_SCALE] = sanitizeFontScale(scale) }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.syncwichDataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.storageValue }
    }

    suspend fun setIngredientChecklistEnabled(enabled: Boolean) {
        context.syncwichDataStore.edit { prefs -> prefs[KEY_INGREDIENT_CHECKLIST] = enabled }
    }

    /** Atomically records the first sync as complete and updates the ordinary sync metadata. */
    suspend fun recordInitialSyncSuccess() {
        context.syncwichDataStore.edit { prefs ->
            prefs[KEY_INITIAL_SYNC_COMPLETED] = true
            prefs[KEY_LAST_SYNC_AT] = System.currentTimeMillis()
            prefs.remove(KEY_LAST_SYNC_ERROR)
        }
    }

    /**
     * Clears sync bookkeeping when [dev.pschmitt.syncwich.data.repository.AccountRepository] signs
     * out.
     */
    suspend fun resetSyncState() {
        context.syncwichDataStore.edit { prefs ->
            prefs.remove(KEY_INITIAL_SYNC_COMPLETED)
            prefs.remove(KEY_LAST_SYNC_AT)
            prefs.remove(KEY_LAST_SYNC_ERROR)
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
        val KEY_NAV_BAR_ORDER = stringPreferencesKey("navigation_bar_order")
        val KEY_NAV_BAR_HIDDEN_ITEMS = stringPreferencesKey("navigation_bar_hidden_items")
        val KEY_FONT_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("font_scale")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_INGREDIENT_CHECKLIST = booleanPreferencesKey("ingredient_checklist")
        val KEY_NAV_BAR_SHOWN_ITEMS = stringPreferencesKey("navigation_bar_shown_items")
        val KEY_INITIAL_SYNC_COMPLETED = booleanPreferencesKey("initial_sync_completed")
        val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val KEY_LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
        val KEY_SYNC_ONLY_ON_WIFI = booleanPreferencesKey("sync_only_on_wifi")
        val KEY_SYNC_WHILE_ROAMING = booleanPreferencesKey("sync_while_roaming")
        val KEY_SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
    }
}
