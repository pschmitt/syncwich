package dev.pschmitt.syncwich.data.settings

import kotlinx.serialization.Serializable

/** Non-secret DataStore values included in a portable Syncwich backup. */
@Serializable
data class SettingsBackupSnapshot(
    val navigationBarOrder: List<String> = emptyList(),
    val navigationBarHiddenItems: Set<String> = emptySet(),
    val navigationBarShownItems: Set<String> = emptySet(),
    /** The destinations visible at export time, including cache-derived defaults. */
    val navigationBarVisibleItems: Set<String> = emptySet(),
    val fontScale: Float = DEFAULT_FONT_SCALE,
    val themeMode: String? = null,
    val ingredientChecklistEnabled: Boolean = false,
    val initialSyncCompleted: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastSyncError: String? = null,
    val syncOnlyOnWifi: Boolean = false,
    val syncWhileRoaming: Boolean = true,
    val syncIntervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
    val syncOnAppStart: Boolean = DEFAULT_SYNC_ON_APP_START,
    val scheduledBackupEnabled: Boolean = false,
    val scheduledBackupFrequency: String = BackupFrequency.Weekly.storageValue,
    val scheduledBackupFolderUri: String? = null,
    val lastBackupAt: Long? = null,
    val lastBackupError: String? = null,
)
