package dev.pschmitt.syncwich.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `unknown stored values fall back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("sepia"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
    }

    @Test
    fun `theme modes have stable persisted values`() {
        assertEquals("system", ThemeMode.SYSTEM.storageValue)
        assertEquals("light", ThemeMode.LIGHT.storageValue)
        assertEquals("dark", ThemeMode.DARK.storageValue)
    }
}
