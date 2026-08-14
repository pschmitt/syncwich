package dev.pschmitt.syncwich.ui.cookbooks

import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CookbookSearchTest {

    private fun cookbook(name: String, description: String = "") =
        CookbookEntity(
            id = name,
            name = name,
            slug = name.lowercase(),
            description = description,
            position = 0,
        )

    @Test
    fun `blank query returns every cookbook unchanged`() {
        val cookbooks = listOf(cookbook("Desserts"), cookbook("Weeknight meals"))

        assertEquals(cookbooks, filterCookbooksByQuery(cookbooks, "  "))
    }

    @Test
    fun `query matches cookbook name case insensitively`() {
        val cookbooks = listOf(cookbook("Desserts"), cookbook("Weeknight meals"))

        assertEquals(listOf(cookbooks[0]), filterCookbooksByQuery(cookbooks, "DESS"))
    }

    @Test
    fun `query matches cookbook description`() {
        val cookbooks =
            listOf(
                cookbook("Favorites", description = "Recipes for quick dinners"),
                cookbook("Baking", description = "Sweet treats"),
            )

        assertEquals(listOf(cookbooks[0]), filterCookbooksByQuery(cookbooks, "quick"))
    }

    @Test
    fun `query matching nothing returns an empty list`() {
        assertEquals(
            emptyList<CookbookEntity>(),
            filterCookbooksByQuery(listOf(cookbook("Baking")), "soup"),
        )
    }
}
