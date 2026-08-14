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
                                    "![Duplicate](https://images.example/one.webp) " +
                                    "<img src=\"/api/media/recipes/recipe-1/assets/step.jpg\" />"
                        ),
                        RecipeInstructionDto(text = "![Unsafe](javascript:alert(1))"),
                    ),
            )

        assertEquals(
            listOf(
                "https://mealie.example/api/media/recipes/recipe-1/images/min-original.webp?v=cover-marker",
                "https://images.example/one.webp",
                "https://mealie.example/api/media/recipes/recipe-1/assets/step.jpg",
            ),
            recipeImageGalleryUrls("https://mealie.example", recipe),
        )
    }

    @Test
    fun `image index shares parsed references with the gallery`() {
        val recipe =
            RecipeDetailDto(
                id = "recipe-1",
                slug = "recipe-one",
                name = "Recipe One",
                recipeInstructions =
                    listOf(
                        RecipeInstructionDto(
                            text =
                                "<img src=\"/api/media/recipes/recipe-1/assets/step.jpg\" alt=\"Step\" />"
                        )
                    ),
            )

        val index = recipeImageIndex("https://mealie.example", recipe)

        assertEquals(
            listOf("https://mealie.example/api/media/recipes/recipe-1/assets/step.jpg"),
            index.galleryUrls,
        )
        assertEquals("Step", index.instructionReferences.single().single().altText)
    }
}
