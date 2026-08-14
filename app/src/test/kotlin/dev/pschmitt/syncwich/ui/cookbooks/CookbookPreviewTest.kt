package dev.pschmitt.syncwich.ui.cookbooks

import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookbookPreviewTest {

    @Test
    fun `cookbook grid keeps narrow screens to one column and grows responsively`() {
        assertEquals(1, cookbookGridColumnCount(360))
        assertEquals(2, cookbookGridColumnCount(512))
        assertEquals(3, cookbookGridColumnCount(744))
    }

    @Test
    fun `carousel uses responsive multi browse dimensions`() {
        assertTrue(COOKBOOK_GRID_MIN_CARD_WIDTH_DP > 160)
        assertTrue(COOKBOOK_PREVIEW_PREFERRED_ITEM_WIDTH_DP > 96)
        assertTrue(COOKBOOK_PREVIEW_ITEM_HEIGHT_DP > 76)
        assertEquals(8, COOKBOOK_PREVIEW_ITEM_SPACING_DP)
        assertEquals(16, COOKBOOK_PREVIEW_CONTENT_PADDING_DP)
    }

    @Test
    fun `filters recipes without a cover value while retaining imported sentinel covers`() {
        val recipes =
            listOf(
                recipe("missing", image = null),
                recipe("sentinel", image = "no image"),
                recipe("blank", image = "  "),
                recipe("has-image", image = "130"),
            )

        assertEquals(
            listOf("sentinel", "has-image"),
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

    @Test
    fun `preview list filters images before applying the carousel limit`() {
        val recipes =
            listOf(
                recipe("missing", image = null),
                recipe("one", image = "130"),
                recipe("two", image = "131"),
                recipe("three", image = "132"),
                recipe("four", image = "133"),
                recipe("five", image = "134"),
                recipe("six", image = "135"),
            )

        assertEquals(
            listOf("one", "two", "three", "four", "five"),
            cookbookPreviewRecipes(recipes, serverUrl = "https://mealie.example.com")
                .map(RecipeSummaryEntity::id),
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
