package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeTitleImageTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun titleImageUsesACardAndOpensTheImageViewer() {
        var clicked = 0
        composeTestRule.setContent {
            MaterialTheme {
                RecipeTitleImage(
                    imageUrl = "https://example.invalid/recipe.jpg",
                    recipeName = "Example recipe",
                    onClick = { clicked++ },
                )
            }
        }

        composeTestRule.onNodeWithTag("recipe-title-image-card").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open recipe images").performClick()

        assertEquals(1, clicked)
    }
}
