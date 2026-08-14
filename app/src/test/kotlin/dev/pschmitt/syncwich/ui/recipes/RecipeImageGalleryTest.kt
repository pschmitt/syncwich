package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import dev.pschmitt.syncwich.data.image.RecipeImageReference
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

    @Test
    fun `viewer items add source and alt text metadata without duplicating images`() {
        val index =
            RecipeImageIndex(
                coverUrl = "https://example.test/cover.jpg",
                galleryUrls =
                    listOf(
                        "https://example.test/cover.jpg",
                        "https://example.test/step.jpg",
                    ),
                instructionReferences =
                    listOf(
                        listOf(
                            RecipeImageReference(
                                url = "https://example.test/step.jpg",
                                altText = "Sear the pork",
                            )
                        )
                    ),
            )

        assertEquals(
            listOf(
                RecipeViewerImage(
                    url = "https://example.test/cover.jpg",
                    title = "Recipe One",
                    sourceLabel = "Recipe cover",
                ),
                RecipeViewerImage(
                    url = "https://example.test/step.jpg",
                    title = "Recipe One",
                    sourceLabel = "Step 1 image",
                    altText = "Sear the pork",
                ),
            ),
            recipeViewerImages("Recipe One", index),
        )
    }

    @Test
    fun `metadata rows include dimensions when the image has loaded`() {
        val image =
            RecipeViewerImage(
                url = "https://example.test/step.jpg",
                title = "Recipe One",
                sourceLabel = "Step 2 image",
                altText = "Sear the pork",
            )

        assertEquals(
            listOf(
                "Source" to "Step 2 image",
                "Description" to "Sear the pork",
                "Dimensions" to "1600 × 900 px",
            ),
            imageMetadataRows(image, ImageDimensions(width = 1600, height = 900)),
        )
    }
}
