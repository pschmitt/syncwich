package dev.pschmitt.syncwich.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshStateTest {

    @Test
    fun `successful refresh has no user-facing error`() {
        assertNull(refreshErrorMessage(Result.success(Unit)))
    }

    @Test
    fun `failed refresh explains that cached data is still shown`() {
        assertEquals(
            "Couldn't refresh. Showing saved data. Check your connection.",
            refreshErrorMessage(Result.failure(IllegalStateException("offline"))),
        )
    }
}
