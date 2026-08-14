package dev.pschmitt.syncwich.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeFormattingTest {

    @Test
    fun `ratings are rounded to one decimal place`() {
        assertEquals("4.7", formatRating(4.6666667))
        assertEquals("4.6", formatRating(4.64))
    }

    @Test
    fun `whole ratings omit the decimal suffix`() {
        assertEquals("5", formatRating(5.0))
        assertEquals("0", formatRating(0.0))
    }
}
