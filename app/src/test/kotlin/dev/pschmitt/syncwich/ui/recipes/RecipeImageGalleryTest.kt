package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeImageGalleryTest {

    @Test
    fun `gallery includes cover and distinct safe step images`() {
        val recipe =
            RecipeDetailDto(
                id = "recipe-1",
                slug = "recipe-one",
                name = "Recipe One",
                image = "cover-marker",
                recipeInstructions =
                    listOf(
                        RecipeInstructionDto(
                            text =
                                "![Step one](https://images.example/one.webp) " +
                                    "![Duplicate](https://images.example/one.webp)",
                        ),
                        RecipeInstructionDto(text = "![Unsafe](javascript:alert(1))"),
                    ),
            )

        assertEquals(
            listOf(
                "https://mealie.example/api/media/recipes/recipe-1/images/min-original.webp?v=cover-marker",
                "https://images.example/one.webp",
            ),
            recipeImageGalleryUrls("https://mealie.example", recipe),
        )
    }
}
