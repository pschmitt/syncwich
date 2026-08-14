package dev.pschmitt.syncwich.ui.settings

import dev.pschmitt.syncwich.data.api.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CredentialsDisplayTest {

    @Test
    fun `display name prefers the most useful available identity`() {
        assertEquals(
            "Ada Lovelace",
            UserDto(username = "ada", fullName = "Ada Lovelace").displayName(),
        )
        assertEquals("ada", UserDto(username = "ada").displayName())
        assertEquals("authenticated user", UserDto().displayName())
    }

    @Test
    fun `credential test subtitle reports success and failure`() {
        assertEquals(
            "Signed in as Ada",
            credentialsTestSubtitle(CredentialsTestState.Success("Ada")),
        )
        assertEquals(
            "Server unavailable",
            credentialsTestSubtitle(CredentialsTestState.Error("Server unavailable")),
        )
    }
}
