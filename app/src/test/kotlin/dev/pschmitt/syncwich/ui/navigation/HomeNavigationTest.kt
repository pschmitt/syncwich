package dev.pschmitt.syncwich.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationTest {

    @Test
    fun `home is the only destination that resets its child stack`() {
        assertTrue(shouldResetHomeStack(TopLevelDestination.HOME))
        assertFalse(shouldResetHomeStack(TopLevelDestination.RECIPES))
        assertFalse(shouldResetHomeStack(TopLevelDestination.SETTINGS))
    }
}
