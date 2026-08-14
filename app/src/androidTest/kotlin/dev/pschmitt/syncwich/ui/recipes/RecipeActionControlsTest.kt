package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
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
                RecipeOverflowMenu(
                    expanded = true,
                    actions = RecipeActionUiState(),
                    onDismiss = {},
                    onFavoriteClick = { favorite = it },
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                    onShareClick = {},
                    onOpenBrowserClick = {},
                )
                RecipeActionControls(actions = RecipeActionUiState(), onRatingSelected = { rating = it })
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
                RecipeOverflowMenu(
                    expanded = true,
                    actions = RecipeActionUiState(),
                    onDismiss = {},
                    onFavoriteClick = {},
                    onMadeThisClick = { madeThisCalls++ },
                    onOpenTimelineClick = { openTimelineCalls++ },
                    onShareClick = {},
                    onOpenBrowserClick = {},
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
                    onRatingSelected = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Saved offline; sync pending").assertIsDisplayed()
    }

    @Test
    fun ratingControlsStayCompactWithout_a_redundant_your_rating_row() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(actions = RecipeActionUiState(), onRatingSelected = {})
            }
        }

        composeTestRule.onAllNodesWithText("Your rating").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Rate 1 out of 5 stars").assertIsDisplayed()
    }
}
