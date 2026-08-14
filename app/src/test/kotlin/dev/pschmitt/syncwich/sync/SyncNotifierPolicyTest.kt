package dev.pschmitt.syncwich.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncNotifierPolicyTest {

    @Test
    fun `foreground sync never posts a background notification`() {
        assertFalse(
            shouldPostBackgroundNotification(appInForeground = true, notificationsAllowed = true)
        )
    }

    @Test
    fun `denied notification permission suppresses background notification`() {
        assertFalse(
            shouldPostBackgroundNotification(appInForeground = false, notificationsAllowed = false)
        )
    }

    @Test
    fun `background sync posts only when notification permission is granted`() {
        assertTrue(
            shouldPostBackgroundNotification(appInForeground = false, notificationsAllowed = true)
        )
    }
}
