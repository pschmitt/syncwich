package dev.pschmitt.syncwich.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRecipeCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun recipeCardsKeepTheSameHeightAndRemainReadable() {
        composeTestRule.setContent {
            MaterialTheme {
                HomeRecipeCard(
                    recipe = recipe("Short", rating = null),
                    serverUrl = "",
                    onClick = {},
                )
                HomeRecipeCard(
                    recipe = recipe("Long recipe title", rating = 4.7),
                    serverUrl = "",
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Short").assertIsDisplayed()
        composeTestRule.onNodeWithText("Long recipe title").assertIsDisplayed()
        val cards = composeTestRule.onAllNodesWithTag("home-recipe-card").fetchSemanticsNodes()

        assertEquals(2, cards.size)
        assertEquals(cards[0].boundsInRoot.height, cards[1].boundsInRoot.height, 0.1f)
    }

    private fun recipe(name: String, rating: Double?): RecipeSummaryEntity =
        RecipeSummaryEntity(
            id = name,
            slug = name,
            name = name,
            description = "",
            image = null,
            rating = rating,
            prepTime = null,
            totalTime = null,
            dateAdded = null,
            lastMade = null,
        )
}
