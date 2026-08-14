package dev.pschmitt.syncwich.ui.cookbooks

import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CookbookPreviewTest {

    @Test
    fun `filters recipes without usable cover images`() {
        val recipes =
            listOf(
                recipe("missing", image = null),
                recipe("sentinel", image = "no image"),
                recipe("blank", image = "  "),
                recipe("has-image", image = "130"),
            )

        assertEquals(
            listOf("has-image"),
            filterRecipePreviewsWithImages(recipes, serverUrl = "https://mealie.example.com")
                .map(RecipeSummaryEntity::id),
        )
    }

    @Test
    fun `filters every preview when the server URL is unusable`() {
        val recipes = listOf(recipe("one", image = "130"), recipe("two", image = "131"))

        assertEquals(
            emptyList<RecipeSummaryEntity>(),
            filterRecipePreviewsWithImages(recipes, serverUrl = ""),
        )
    }

    private fun recipe(id: String, image: String?) =
        RecipeSummaryEntity(
            id = id,
            slug = id,
            name = id,
            description = "",
            image = image,
            rating = null,
            prepTime = null,
            totalTime = null,
            dateAdded = null,
            lastMade = null,
        )
}
