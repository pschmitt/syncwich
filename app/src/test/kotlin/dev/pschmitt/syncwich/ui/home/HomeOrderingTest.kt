package dev.pschmitt.syncwich.ui.home

import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOrderingTest {

    @Test
    fun `recent recipes sort newest first and keep undated recipes last`() {
        val recipes =
            listOf(
                recipe("older", "2026-01-02"),
                recipe("newest", "2026-06-13"),
                recipe("undated", null),
                recipe("middle", "2026-03-09"),
            )

        assertEquals(
            listOf("newest", "middle", "older", "undated"),
            sortRecipesByDate(recipes, RecipeSummaryEntity::dateAdded, limit = 10).map { it.name },
        )
    }

    @Test
    fun `recent recipe previews are capped`() {
        val recipes =
            (1..6).map { recipe("recipe-$it", "2026-06-${it.toString().padStart(2, '0')}") }

        assertEquals(
            listOf("recipe-6", "recipe-5", "recipe-4", "recipe-3", "recipe-2"),
            sortRecipesByDate(recipes, RecipeSummaryEntity::dateAdded).map { it.name },
        )
    }

    @Test
    fun `favorite recipe previews sort alphabetically and are capped`() {
        val recipes =
            listOf(
                recipe("Ziti", null),
                recipe("apple pie", null),
                recipe("Bread", null),
            )

        assertEquals(
            listOf("apple pie", "Bread"),
            sortFavoriteRecipes(recipes, limit = 2).map { it.name },
        )
    }

    @Test
    fun `recently viewed follows history order and omits uncached recipes`() {
        val recipes = listOf(recipe("one", null), recipe("three", null), recipe("two", null))

        assertEquals(
            listOf("two", "one"),
            recipesForHistory(listOf("missing", "two", "one", "three"), recipes, limit = 2).map {
                it.name
            },
        )
    }

    private fun recipe(name: String, dateAdded: String?): RecipeSummaryEntity =
        RecipeSummaryEntity(
            id = name,
            slug = name,
            name = name,
            description = "",
            image = null,
            rating = null,
            prepTime = null,
            totalTime = null,
            dateAdded = dateAdded,
            lastMade = null,
        )
}
