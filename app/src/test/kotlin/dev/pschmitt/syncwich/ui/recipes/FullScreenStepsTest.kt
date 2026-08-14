package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.image.RecipeImageReference
import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenStepsTest {

    @Test
    fun `full screen step image list preserves order and removes duplicates`() {
        val references =
            listOf(
                listOf(RecipeImageReference("https://example.test/one.jpg")),
                listOf(
                    RecipeImageReference("https://example.test/two.jpg"),
                    RecipeImageReference("https://example.test/one.jpg"),
                ),
            )

        assertEquals(
            listOf(
                "https://example.test/one.jpg",
                "https://example.test/two.jpg",
            ),
            fullScreenStepImageUrls(references),
        )
    }

    @Test
    fun `transient step font scale stays within accessible bounds`() {
        assertEquals(0.8f, adjustStepFontScale(0.8f, -0.1f))
        assertEquals(1.1f, adjustStepFontScale(1.0f, 0.1f))
        assertEquals(1.6f, adjustStepFontScale(1.6f, 0.1f))
    }

    @Test
    fun `full screen steps leave room for the floating controls`() {
        assertEquals(96, FULL_SCREEN_STEPS_BOTTOM_PADDING_DP)
    }
}
