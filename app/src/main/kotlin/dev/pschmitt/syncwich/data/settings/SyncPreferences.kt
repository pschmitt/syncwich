package dev.pschmitt.syncwich.data.settings

/** User-controlled policy for WorkManager's periodic background sync. */
data class SyncPreferences(
    val syncOnlyOnWifi: Boolean = false,
    val syncWhileRoaming: Boolean = true,
    val syncIntervalHours: Int = DEFAULT_SYNC_INTERVAL_HOURS,
)

const val DEFAULT_SYNC_INTERVAL_HOURS = 6
const val MIN_SYNC_INTERVAL_HOURS = 1
const val MAX_SYNC_INTERVAL_HOURS = 24
const val DEFAULT_SYNC_ON_APP_START = true

internal fun sanitizeSyncIntervalHours(hours: Int): Int =
    hours.coerceIn(MIN_SYNC_INTERVAL_HOURS, MAX_SYNC_INTERVAL_HOURS)
