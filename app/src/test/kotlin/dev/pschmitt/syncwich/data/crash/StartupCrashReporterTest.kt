package dev.pschmitt.syncwich.data.crash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupCrashReporterTest {

    @Test
    fun `crash text redacts URLs and credential-like values`() {
        val sanitized =
            sanitizeCrashText(
                "request https://mealie.example/api/recipes Authorization: Bearer super-secret"
            )

        assertFalse(sanitized.contains("mealie.example"))
        assertFalse(sanitized.contains("super-secret"))
        assertTrue(sanitized.contains("<redacted-url>"))
        assertTrue(sanitized.contains("Authorization: <redacted>"))
    }

    @Test
    fun `crash formatting preserves exception type and stack`() {
        val details = formatStartupCrash(IllegalStateException("boom"))

        assertTrue(details.contains("IllegalStateException"))
        assertTrue(details.contains("boom"))
    }
}
