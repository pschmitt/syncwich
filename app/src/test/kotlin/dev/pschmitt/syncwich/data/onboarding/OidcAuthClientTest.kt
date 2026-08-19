package dev.pschmitt.syncwich.data.onboarding

import dev.pschmitt.syncwich.data.settings.MealieAuthMethod
import dev.pschmitt.syncwich.data.settings.MealieCredentials
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OidcAuthClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OidcAuthClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OidcAuthClient(OkHttpClient(), Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `authorization URL is rooted at the entered server`() {
        val result = client.authorizationUrl(server.url("/mealie/").toString())

        assertEquals(
            "${server.url("/mealie")}/api/auth/oauth",
            result.getOrThrow(),
        )
    }

    @Test
    fun `callback code is exchanged with the WebView state cookie`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"mealie-jwt\",\"token_type\":\"bearer\"}")
        )

        val result =
            client.exchangeCallback(
                server.url("/").toString(),
                "${server.url("/")}login?code=oauth-code&state=state-value",
                "oidc_state=opaque-state",
            )

        assertEquals("mealie-jwt", result.getOrThrow())
        val request = server.takeRequest()
        assertEquals("/api/auth/oauth/callback?code=oauth-code&state=state-value", request.path)
        assertEquals("oidc_state=opaque-state", request.getHeader("Cookie"))
    }

    @Test
    fun `callback from another origin is rejected before network access`() = runTest {
        val result =
            client.exchangeCallback(
                server.url("/").toString(),
                "https://attacker.example/login?code=leaked&state=state",
                "secret-cookie",
            )

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `OIDC JWT is considered expiring inside the refresh window`() {
        val now = 1_000_000L
        val token = jwtWithExpiry((now / 1_000L) + 60)

        assertTrue(client.isExpiringSoon(token, now))
        assertFalse(client.isExpiringSoon(jwtWithExpiry((now / 1_000L) + 600), now))
    }

    @Test
    fun `refresh uses the saved OIDC JWT`() = runTest {
        server.enqueue(
            MockResponse().setBody("{\"access_token\":\"renewed-jwt\"}")
        )
        val credentials =
            MealieCredentials(
                serverUrl = server.url("/").toString(),
                apiToken = "current-jwt",
                authMethod = MealieAuthMethod.Oidc,
            )

        assertEquals("renewed-jwt", client.refresh(credentials).getOrThrow())
        val request = server.takeRequest()
        assertEquals("/api/auth/refresh", request.path)
        assertEquals("Bearer current-jwt", request.getHeader("Authorization"))
    }

    private fun jwtWithExpiry(expirySeconds: Long): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"exp\":$expirySeconds}".toByteArray())
        return "header.$payload.signature"
    }
}
