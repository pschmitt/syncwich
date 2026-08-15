package dev.pschmitt.syncwich.sync

import dev.pschmitt.syncwich.ui.home.formatRelativeSyncTime
import dev.pschmitt.syncwich.ui.home.syncStatusDetails
import dev.pschmitt.syncwich.ui.home.syncStatusHeadline
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusTest {

    @Test
    fun `running sync takes precedence over an older error`() {
        val status =
            resolveSyncStatus(
                isSyncing = true,
                lastSyncAt = 1_000L,
                errorMessage = "offline",
                nowMillis = TimeUnit.HOURS.toMillis(20),
                currentMessage = "Refreshing cookbooks…",
            )

        assertEquals(SyncStatusState.SYNCING, status.state)
        assertEquals("Refreshing cookbooks…", syncStatusHeadline(status))
    }

    @Test
    fun `failed sync exposes the error without hiding the cached timestamp`() {
        val status =
            resolveSyncStatus(
                isSyncing = false,
                lastSyncAt = 10_000L,
                errorMessage = "Server unavailable",
                nowMillis = 20_000L,
            )

        assertEquals(SyncStatusState.ERROR, status.state)
        assertEquals("Server unavailable", syncStatusDetails(status, nowMillis = 20_000L))
        assertEquals(10_000L, status.lastSyncAt)
    }

    @Test
    fun `successful cache becomes stale only after the configured grace period`() {
        val status =
            resolveSyncStatus(
                isSyncing = false,
                lastSyncAt = 1_000L,
                errorMessage = null,
                nowMillis = 1_000L + TimeUnit.HOURS.toMillis(12),
            )

        assertEquals(SyncStatusState.STALE, status.state)
        assertEquals(
            "Last synced 12h ago. Sync when a connection is available.",
            syncStatusDetails(status, nowMillis = 1_000L + TimeUnit.HOURS.toMillis(12)),
        )
    }

    @Test
    fun `relative sync time handles missing and recent timestamps`() {
        assertEquals("Last synced: never", formatRelativeSyncTime(null, 10_000L))
        assertEquals("Last synced just now", formatRelativeSyncTime(9_500L, 10_000L))
        assertEquals("Last synced 2h ago", formatRelativeSyncTime(10_000L, 7_210_000L))
    }

    @Test
    fun `cached recipes do not present as not synced yet`() {
        val status =
            resolveSyncStatus(
                    isSyncing = false,
                    lastSyncAt = null,
                    errorMessage = null,
                    nowMillis = 10_000L,
                )
                .copy(hasCachedData = true)

        assertEquals("Saved recipes ready", syncStatusHeadline(status))
        assertEquals(
            "Showing saved recipes; updates will be checked in the background.",
            syncStatusDetails(status, nowMillis = 10_000L),
        )
    }
}
