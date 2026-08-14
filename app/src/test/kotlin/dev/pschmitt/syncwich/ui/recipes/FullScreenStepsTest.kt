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
}
