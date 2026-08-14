package dev.pschmitt.syncwich.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AboutBuildTypeTest {

    @Test
    fun `build type label distinguishes debug and release`() {
        assertEquals("Debug build", aboutBuildTypeLabel(isDebug = true))
        assertEquals("Release build", aboutBuildTypeLabel(isDebug = false))
    }
}
