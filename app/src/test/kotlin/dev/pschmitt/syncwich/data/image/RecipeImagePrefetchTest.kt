package dev.pschmitt.syncwich.data.image

import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImagePrefetchTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `extracts unique absolute http images and ignores unreliable destinations`() {
        val markdown =
            """
            ![one](https://images.example/one.jpg "title")
            ![one again](https://images.example/one.jpg)
            ![two](<http://images.example/two.jpg>)
            [ordinary link](https://images.example/not-an-image.jpg)
            ![relative](images/relative.jpg)
            ![data](data:image/png;base64,abc)
            """.trimIndent()

        assertEquals(
            listOf(
                "https://images.example/one.jpg",
                "http://images.example/two.jpg",
            ),
            extractMarkdownImageUrls(markdown),
        )
    }

    @Test
    fun `accepts only bounded absolute http images without embedded credentials`() {
        assertTrue(isSafeRecipeImageUrl("https://images.example/step.jpg?size=large"))
        assertTrue(isSafeRecipeImageUrl("HTTP://images.example/step.jpg"))
        assertFalse(isSafeRecipeImageUrl("images/step.jpg"))
        assertFalse(isSafeRecipeImageUrl("data:image/png;base64,abc"))
        assertFalse(isSafeRecipeImageUrl("ftp://images.example/step.jpg"))
        assertFalse(isSafeRecipeImageUrl("https://user:password@images.example/step.jpg"))
        assertFalse(isSafeRecipeImageUrl("https://"))
        assertFalse(isSafeRecipeImageUrl("https://images.example/${"x".repeat(2_049)}"))
    }

    @Test
    fun `selects every cover before capped inline images`() {
        val recipes = listOf(recipeSummary("r1", "1"), recipeSummary("r2", "2"))
        val details =
            listOf(
                detail(
                    "r1",
                    """
                    {"id":"r1","slug":"r1","name":"One","recipeInstructions":[
                      {"text":"![a](https://images.example/a.jpg) ![b](https://images.example/b.jpg)"}
                    ]}
                    """,
                ),
                detail(
                    "r2",
                    """
                    {"id":"r2","slug":"r2","name":"Two","recipeInstructions":[
                      {"text":"![c](https://images.example/c.jpg)"}
                    ]}
                    """,
                ),
            )

        assertEquals(
            listOf(
                "https://mealie.example/api/media/recipes/r1/images/min-original.webp",
                "https://mealie.example/api/media/recipes/r2/images/min-original.webp",
                "https://images.example/a.jpg",
                "https://images.example/c.jpg",
            ),
            selectRecipeImagePrefetchUrls(
                serverUrl = "https://mealie.example",
                recipes = recipes,
                details = details,
                json = json,
                maxInlineImagesPerRecipe = 1,
                maxInlineImages = 2,
            ),
        )
    }

    @Test
    fun `skips malformed cached detail json without losing covers`() {
        val urls =
            selectRecipeImagePrefetchUrls(
                serverUrl = "https://mealie.example",
                recipes = listOf(recipeSummary("keep", "1")),
                details = listOf(detail("broken", "not json")),
                json = json,
            )

        assertEquals(
            listOf(
                "https://mealie.example/api/media/recipes/keep/images/min-original.webp"
            ),
            urls,
        )
    }

    @Test
    fun `continues after a failed prefetch and deduplicates urls`() = runTest {
        val attempted = mutableListOf<String>()

        val stats =
            prefetchImageUrls(listOf("one", "two", "one"), maxConcurrent = 1) { url ->
                attempted += url
                url != "two"
            }

        assertEquals(listOf("one", "two"), attempted)
        assertEquals(ImagePrefetchStats(attempted = 2, succeeded = 1, failed = 1), stats)
        assertTrue(stats.failed > 0)
    }

    private fun recipeSummary(id: String, image: String) =
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

    private fun detail(id: String, detailJson: String) =
        RecipeDetailEntity(id = id, slug = id, detailJson = detailJson, fetchedAt = 0)
}
