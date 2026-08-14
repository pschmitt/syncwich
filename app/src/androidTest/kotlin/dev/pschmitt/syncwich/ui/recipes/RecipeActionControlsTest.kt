package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeActionControlsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun favoriteAndRatingControlsExposeLabelsAndCallbacks() {
        var favorite: Boolean? = null
        var rating: Int? = null
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(
                    actions = RecipeActionUiState(),
                    onFavoriteClick = { favorite = it },
                    onRatingSelected = { rating = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Favorite").performClick()
        composeTestRule.onNodeWithContentDescription("Rate 4 out of 5 stars").performClick()

        assertEquals(true, favorite)
        assertEquals(4, rating)
    }

    @Test
    fun unsupportedActionsAreClearlyPendingAndDisabled() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(
                    actions = RecipeActionUiState(),
                    onFavoriteClick = {},
                    onRatingSelected = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("I made this (pending)")
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithText("Open timeline (pending)")
            .assertIsNotEnabled()
    }
}
