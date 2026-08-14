package dev.pschmitt.syncwich.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSchedulerTest {

    @Test
    fun `automatic startup waits briefly when cached data exists`() {
        assertEquals(SyncScheduler.STARTUP_SYNC_DELAY_MILLIS, startupSyncDelayMillis(true))
    }

    @Test
    fun `automatic startup is immediate when cache is empty`() {
        assertEquals(0L, startupSyncDelayMillis(false))
    }
}
