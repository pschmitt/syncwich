package dev.pschmitt.syncwich.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPreferencesTest {

    @Test
    fun `sync on app start defaults to enabled`() {
        assertTrue(DEFAULT_SYNC_ON_APP_START)
    }

    @Test
    fun `interval is clamped to WorkManager-safe user-facing bounds`() {
        assertEquals(MIN_SYNC_INTERVAL_HOURS, sanitizeSyncIntervalHours(0))
        assertEquals(12, sanitizeSyncIntervalHours(12))
        assertEquals(MAX_SYNC_INTERVAL_HOURS, sanitizeSyncIntervalHours(48))
    }

    @Test
    fun `defaults preserve existing connected six-hour background behavior`() {
        assertEquals(SyncPreferences(), SyncPreferences(false, true, DEFAULT_SYNC_INTERVAL_HOURS))
    }
}
