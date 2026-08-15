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

    @Test
    fun `serving counts omit trailing zeroes`() {
        assertEquals("4", formatServings(4.0))
        assertEquals("2.5", formatServings(2.5))
    }

    @Test
    fun `step content does not repeat its leading ordered-list number`() {
        assertEquals("Preheat the oven.", stripLeadingStepNumber("1. Preheat the oven."))
        assertEquals("Mix the sauce.", stripLeadingStepNumber("2) Mix the sauce."))
        assertEquals(
            "Fold the dough.",
            stripLeadingStepNumber("<ol><li>Fold the dough.</li></ol>"),
        )
        assertEquals("1.5 kg of flour.", stripLeadingStepNumber("1.5 kg of flour."))
    }
}
