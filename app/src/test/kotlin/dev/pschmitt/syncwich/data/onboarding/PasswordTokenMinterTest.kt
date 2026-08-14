package dev.pschmitt.syncwich.data.onboarding

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordTokenMinterTest {

    private lateinit var server: MockWebServer
    private lateinit var minter: PasswordTokenMinter

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        minter = PasswordTokenMinter(OkHttpClient(), Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login JWT is exchanged for the returned long-lived token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"short-lived-jwt\",\"token_type\":\"bearer\"}"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"name\":\"Syncwich (test)\",\"id\":2," +
                        "\"createdAt\":\"2026-08-14T00:00:00Z\",\"token\":\"long-lived-token\"}",
                ),
        )

        val result =
            minter.mintToken(
                server.url("/").toString(),
                username = "ai@example.test",
                password = "not-persisted",
                tokenName = "Syncwich (test)",
            )

        assertTrue("result=$result", result.isSuccess)
        assertEquals("long-lived-token", result.getOrThrow())

        val login = server.takeRequest()
        assertEquals("POST", login.method)
        assertEquals("/api/auth/token", login.path)
        assertTrue(login.getHeader("Content-Type").orEmpty().startsWith("application/x-www-form-urlencoded"))
        val loginBody = login.body.readUtf8()
        assertTrue(loginBody.contains("username=ai%40example.test"))
        assertTrue(loginBody.contains("password=not-persisted"))

        val tokenRequest = server.takeRequest()
        assertEquals("POST", tokenRequest.method)
        assertEquals("/api/users/api-tokens", tokenRequest.path)
        assertEquals("Bearer short-lived-jwt", tokenRequest.getHeader("Authorization"))
        val tokenBody = tokenRequest.body.readUtf8()
        assertTrue(tokenBody.contains("\"name\":\"Syncwich (test)\""))
        assertTrue(tokenBody.contains("\"integrationId\":\"generic\""))
    }

    @Test
    fun `invalid password stops before token creation`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result =
            minter.mintToken(
                server.url("/").toString(),
                username = "ai@example.test",
                password = "wrong",
                tokenName = "Syncwich (test)",
            )

        assertTrue(result.isFailure)
        assertEquals(
            OnboardingError.Unauthorized,
            (result.exceptionOrNull() as OnboardingValidationException).error,
        )
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `server URL does not need a trailing slash`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"short-lived-jwt\"}"),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"name\":\"Syncwich (test)\",\"id\":2,\"token\":\"long-lived-token\"}"),
        )

        val result =
            minter.mintToken(
                server.url("/").toString().trimEnd('/'),
                username = "ai@example.test",
                password = "not-persisted",
                tokenName = "Syncwich (test)",
            )

        assertTrue("result=$result", result.isSuccess)
        assertEquals("long-lived-token", result.getOrThrow())
        assertEquals("/api/auth/token", server.takeRequest().path)
        assertEquals("/api/users/api-tokens", server.takeRequest().path)
    }
}
