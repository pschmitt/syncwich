package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.ui.common.highlightedSearchText
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSearchTest {

    private fun recipe(name: String, description: String = "") =
        RecipeSummaryEntity(
            id = name,
            slug = name,
            name = name,
            description = description,
            image = null,
            rating = null,
            prepTime = null,
            totalTime = null,
            dateAdded = null,
            lastMade = null,
        )

    @Test
    fun `blank query returns every recipe unchanged`() {
        val recipes = listOf(recipe("Tofu"), recipe("Brownies"))

        assertEquals(recipes, filterRecipesByQuery(recipes, ""))
    }

    @Test
    fun `query matches recipe name case-insensitively`() {
        val recipes = listOf(recipe("Nougat Brownies"), recipe("Silken Tofu"))

        assertEquals(listOf(recipes[0]), filterRecipesByQuery(recipes, "brown"))
    }

    @Test
    fun `query matches recipe description when the name doesn't match`() {
        val recipes = listOf(recipe("Bibimbap", description = "A savory Korean rice dish"))

        assertEquals(recipes, filterRecipesByQuery(recipes, "korean"))
    }

    @Test
    fun `query matching nothing returns an empty list`() {
        val recipes = listOf(recipe("Tofu"))

        assertEquals(emptyList<RecipeSummaryEntity>(), filterRecipesByQuery(recipes, "pizza"))
    }

    @Test
    fun `search matches expose every non-overlapping case-insensitive range`() {
        assertEquals(listOf(0..2, 4..6), recipeSearchMatchRanges("Ban ban", "ban"))
    }

    @Test
    fun `highlighted search text preserves content and adds match spans`() {
        val text = highlightedSearchText("Ban ban", "ban", SpanStyle(color = Color.Red))

        assertEquals("Ban ban", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(0, text.spanStyles[0].start)
        assertEquals(3, text.spanStyles[0].end)
        assertEquals(4, text.spanStyles[1].start)
        assertEquals(7, text.spanStyles[1].end)
    }
}
