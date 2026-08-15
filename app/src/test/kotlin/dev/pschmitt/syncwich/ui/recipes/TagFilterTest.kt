package dev.pschmitt.syncwich.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class TagFilterTest {

    @Test
    fun `filter button reports whether a filter is active`() {
        assertEquals("Filters", recipeFilterButtonLabel(selectedFilterCount = 0))
        assertEquals("Filters (1)", recipeFilterButtonLabel(selectedFilterCount = 1))
        assertEquals("Filters (2)", recipeFilterButtonLabel(selectedFilterCount = 2))
    }
}
