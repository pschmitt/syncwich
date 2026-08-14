package dev.pschmitt.syncwich.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AboutBuildTypeTest {

    @Test
    fun `build type label distinguishes debug and release`() {
        assertEquals("Debug build", aboutBuildTypeLabel(isDebug = true))
        assertEquals("Release build", aboutBuildTypeLabel(isDebug = false))
    }

    @Test
    fun `build revisions resolve to GitHub commit URLs`() {
        assertEquals(
            "https://github.com/pschmitt/syncwich/commit/31f4493",
            githubCommitUrl("31f4493"),
        )
        assertEquals(
            "https://github.com/pschmitt/syncwich/commit/31f4493",
            githubCommitUrl("31f4493-dirty"),
        )
        assertEquals(null, githubCommitUrl("unknown"))
    }
}
