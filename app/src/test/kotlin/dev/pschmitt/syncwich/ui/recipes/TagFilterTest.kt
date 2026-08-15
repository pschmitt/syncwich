package dev.pschmitt.syncwich.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class TagFilterTest {

    @Test
    fun `filter button content description reports whether a filter is active`() {
        assertEquals(
            "Filters",
            recipeFilterButtonContentDescription(selectedFilterCount = 0),
        )
        assertEquals(
            "Filters, 1 active",
            recipeFilterButtonContentDescription(selectedFilterCount = 1),
        )
        assertEquals(
            "Filters, 2 active",
            recipeFilterButtonContentDescription(selectedFilterCount = 2),
        )
    }
}
