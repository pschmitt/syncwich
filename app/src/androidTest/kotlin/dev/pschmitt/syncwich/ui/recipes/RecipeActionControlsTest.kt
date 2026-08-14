package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
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
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Favorite").performClick()
        composeTestRule.onNodeWithContentDescription("Rate 4 out of 5 stars").performClick()

        assertEquals(true, favorite)
        assertEquals(4, rating)
    }

    @Test
    fun madeThisAndTimelineControlsInvokeTheirCallbacks() {
        var madeThisCalls = 0
        var openTimelineCalls = 0
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(
                    actions = RecipeActionUiState(),
                    onFavoriteClick = {},
                    onRatingSelected = {},
                    onMadeThisClick = { madeThisCalls++ },
                    onOpenTimelineClick = { openTimelineCalls++ },
                )
            }
        }

        composeTestRule.onNodeWithText("I made this").performClick()
        composeTestRule.onNodeWithText("Open timeline").performClick()

        assertEquals(1, madeThisCalls)
        assertEquals(1, openTimelineCalls)
    }

    @Test
    fun madeThisPendingStateShowsTheOfflineSyncBanner() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(
                    actions = RecipeActionUiState(madeThisPending = true),
                    onFavoriteClick = {},
                    onRatingSelected = {},
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Saved offline; sync pending").assertIsDisplayed()
    }
}
