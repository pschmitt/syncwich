package dev.pschmitt.syncwich.data.onboarding

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingValidatorTest {

    private lateinit var server: MockWebServer
    private lateinit var validator: OnboardingValidator

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        validator = OnboardingValidator(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `valid token against a reachable server succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"username\":\"ai\"}"))

        val result = validator.validate(server.url("/").toString(), "a-valid-token")

        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("/api/users/self", request.path)
        assertEquals("Bearer a-valid-token", request.getHeader("Authorization"))
    }

    @Test
    fun `rejected token surfaces as Unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = validator.validate(server.url("/").toString(), "a-bad-token")

        val error = (result.exceptionOrNull() as OnboardingValidationException).error
        assertEquals(OnboardingError.Unauthorized, error)
    }

    @Test
    fun `server error surfaces as ServerError with the response code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = validator.validate(server.url("/").toString(), "a-token")

        val error = (result.exceptionOrNull() as OnboardingValidationException).error
        assertEquals(OnboardingError.ServerError(500), error)
    }

    @Test
    fun `malformed url is rejected without a network call`() = runTest {
        val result = validator.validate("not a url", "a-token")

        val error = (result.exceptionOrNull() as OnboardingValidationException).error
        assertEquals(OnboardingError.MalformedUrl, error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `unreachable server surfaces as Unreachable`() = runTest {
        // A dedicated, already-shut-down server so `server` itself (torn down normally in
        // tearDown()) is never shut down twice.
        val deadServer = MockWebServer()
        deadServer.start()
        val deadUrl = deadServer.url("/").toString()
        deadServer.shutdown()

        val result = validator.validate(deadUrl, "a-token")

        val error = (result.exceptionOrNull() as OnboardingValidationException).error
        assertEquals(OnboardingError.Unreachable, error)
    }
}
