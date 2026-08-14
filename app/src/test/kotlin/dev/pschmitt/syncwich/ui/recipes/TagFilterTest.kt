package dev.pschmitt.syncwich.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class TagFilterTest {

    @Test
    fun `tag filter toggle explains collapsed and expanded states`() {
        assertEquals("Show tags (12)", tagFilterToggleLabel(expanded = false, count = 12))
        assertEquals("Hide tags", tagFilterToggleLabel(expanded = true, count = 12))
    }
}
