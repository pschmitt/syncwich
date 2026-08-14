package dev.pschmitt.syncwich.ui.home

import dev.pschmitt.syncwich.sync.SyncStatus
import dev.pschmitt.syncwich.sync.SyncStatusState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRefreshTest {

    @Test
    fun `automatic startup sync stays in the sync card`() {
        assertFalse(
            isHomePullToRefreshActive(
                SyncStatus(state = SyncStatusState.SYNCING),
                userRefreshRequested = false,
            )
        )
    }

    @Test
    fun `user refresh exposes the pull indicator while syncing`() {
        assertTrue(
            isHomePullToRefreshActive(
                SyncStatus(state = SyncStatusState.SYNCING),
                userRefreshRequested = true,
            )
        )
    }
}
