package dev.pschmitt.syncwich.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCompatibilityTest {
    @Test
    fun `debug and release application ids share backup format`() {
        assertTrue(
            isCompatibleApplicationId(
                "dev.pschmitt.syncwich.debug",
                "dev.pschmitt.syncwich",
            )
        )
        assertFalse(isCompatibleApplicationId("dev.example.other", "dev.pschmitt.syncwich"))
    }
}
