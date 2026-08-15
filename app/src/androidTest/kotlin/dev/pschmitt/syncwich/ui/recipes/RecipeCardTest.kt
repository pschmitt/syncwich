package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun recipeCardsHaveTheSameHeightAndFavoriteBadgeIsVisible() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeCard(
                    recipe = recipe("Favorite", image = "v1"),
                    serverUrl = "https://recipes.example",
                    isFavorite = true,
                    onClick = {},
                )
                RecipeCard(
                    recipe = recipe("No image", image = null),
                    serverUrl = "",
                    isFavorite = false,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("No image").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Favorite recipe").assertIsDisplayed()

        val cards =
            composeTestRule
                .onAllNodesWithTag("recipe-card")
                .fetchSemanticsNodes()
        assertEquals(2, cards.size)
        assertEquals(cards[0].boundsInRoot.height, cards[1].boundsInRoot.height, 0.1f)
    }

    private fun recipe(name: String, image: String?): RecipeSummaryEntity =
        RecipeSummaryEntity(
            id = name,
            slug = name,
            name = name,
            description = "",
            image = image,
            rating = null,
            prepTime = null,
            totalTime = null,
            dateAdded = null,
            lastMade = null,
        )
}
