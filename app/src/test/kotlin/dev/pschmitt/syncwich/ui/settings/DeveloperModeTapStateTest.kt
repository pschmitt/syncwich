package dev.pschmitt.syncwich.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DeveloperModeTapStateTest {

    @Test
    fun `seven taps unlock and start a fresh sequence`() {
        val state = DeveloperModeTapState()

        repeat(6) { tap ->
            assertEquals(
                DeveloperModeTapAction.Progress(6 - tap),
                state.onTap(now = tap.toLong() + 1, developerModeEnabled = false),
            )
        }
        assertEquals(
            DeveloperModeTapAction.Unlock,
            state.onTap(now = 7, developerModeEnabled = false),
        )
        assertEquals(
            DeveloperModeTapAction.Progress(6),
            state.onTap(now = 8, developerModeEnabled = false),
        )
    }

    @Test
    fun `persisted developer mode reports already enabled`() {
        assertEquals(
            DeveloperModeTapAction.AlreadyDeveloper,
            DeveloperModeTapState().onTap(now = 1, developerModeEnabled = true),
        )
    }
}
