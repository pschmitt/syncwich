package dev.pschmitt.syncwich.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeColorTest {
    @Test
    fun fallbackPrimaryMatchesLauncherAccentInLightAndDarkThemes() {
        val launcherAccent = Color(0xFFE4572E)

        assertEquals(launcherAccent, SyncwichTerracotta40)
        assertEquals(launcherAccent, LightColors.primary)
        assertEquals(launcherAccent, DarkColors.primary)
    }
}
