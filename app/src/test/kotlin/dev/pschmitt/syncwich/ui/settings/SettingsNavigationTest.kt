package dev.pschmitt.syncwich.ui.settings

import dev.pschmitt.syncwich.ui.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SettingsNavigationTest {

    @Test
    fun `top-level settings categories keep the intended order`() {
        assertEquals(
            listOf(SettingsCategory.Server, SettingsCategory.Appearance, SettingsCategory.Sync),
            SettingsCategory.entries,
        )
    }

    @Test
    fun `category routes preserve the selected submenu`() {
        val serverRoute = Route.SettingsCategory(SettingsCategory.Server)
        val appearanceRoute = Route.SettingsCategory(SettingsCategory.Appearance)

        assertEquals(SettingsCategory.Server, serverRoute.category)
        assertEquals(SettingsCategory.Appearance, appearanceRoute.category)
        assertNotEquals(serverRoute, appearanceRoute)
    }

    @Test
    fun `server connection route is distinct from the category menu`() {
        assertNotEquals(Route.Settings, Route.SettingsConnection)
        assertNotEquals(Route.SettingsCategory(SettingsCategory.Server), Route.SettingsConnection)
    }

    @Test
    fun `api token details never expose the complete token`() {
        assertEquals("••••7890", maskApiToken("secret-7890"))
        assertEquals("••••", maskApiToken("1234"))
        assertEquals("Not configured", maskApiToken(""))
    }
}
