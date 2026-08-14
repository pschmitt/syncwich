package dev.pschmitt.syncwich.sync

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncNetworkPolicyTest {

    @Test
    fun `wifi-only takes precedence over roaming preference`() {
        assertEquals(
            NetworkType.UNMETERED,
            SyncNetworkPolicy.requiredNetworkType(syncOnlyOnWifi = true, syncWhileRoaming = true),
        )
    }

    @Test
    fun `disabled roaming uses non-roaming connected networks`() {
        assertEquals(
            NetworkType.NOT_ROAMING,
            SyncNetworkPolicy.requiredNetworkType(syncOnlyOnWifi = false, syncWhileRoaming = false),
        )
    }

    @Test
    fun `default policy allows any connected network`() {
        assertEquals(
            NetworkType.CONNECTED,
            SyncNetworkPolicy.requiredNetworkType(syncOnlyOnWifi = false, syncWhileRoaming = true),
        )
    }
}
