package dev.pschmitt.syncwich.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class FontSizePreferenceTest {

    @Test
    fun `missing and invalid values use the unchanged default`() {
        assertEquals(DEFAULT_FONT_SCALE, sanitizeFontScale(DEFAULT_FONT_SCALE), 0f)
        assertEquals(DEFAULT_FONT_SCALE, sanitizeFontScale(Float.NaN), 0f)
        assertEquals(DEFAULT_FONT_SCALE, sanitizeFontScale(Float.POSITIVE_INFINITY), 0f)
    }

    @Test
    fun `font scale is clamped to accessible bounds`() {
        assertEquals(MIN_FONT_SCALE, sanitizeFontScale(0.1f), 0f)
        assertEquals(MAX_FONT_SCALE, sanitizeFontScale(2f), 0f)
        assertEquals(1.15f, sanitizeFontScale(1.15f), 0f)
    }
}
