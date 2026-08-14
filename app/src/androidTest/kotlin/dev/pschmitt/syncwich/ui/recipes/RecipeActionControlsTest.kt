package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
                RecipeOverflowMenu(
                    expanded = true,
                    actions = RecipeActionUiState(),
                    onDismiss = {},
                    onFavoriteClick = { favorite = it },
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                    onShareClick = {},
                    onOpenBrowserClick = {},
                    onDeleteClick = {},
                )
                RecipeActionControls(
                    actions = RecipeActionUiState(),
                    globalRating = 4.6666667,
                    onRatingSelected = { rating = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Favorite").performClick()
        composeTestRule.onNodeWithContentDescription("Open rating dialog").performClick()
        composeTestRule.onNodeWithContentDescription("Rate 4 out of 5 stars").performClick()

        assertEquals(true, favorite)
        assertEquals(4, rating)
        composeTestRule.onNodeWithText("4.7 / 5").assertIsDisplayed()
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
                    onDeleteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("I made this").performClick()
        composeTestRule.onNodeWithText("Show timeline").performClick()

        assertEquals(1, madeThisCalls)
        assertEquals(1, openTimelineCalls)
    }

    @Test
    fun deleteMenuItemInvokesDeleteRequest() {
        var deleteMenuCalls = 0
        composeTestRule.setContent {
            MaterialTheme {
                RecipeOverflowMenu(
                    expanded = true,
                    actions = RecipeActionUiState(),
                    onDismiss = {},
                    onFavoriteClick = {},
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                    onShareClick = {},
                    onOpenBrowserClick = {},
                    onDeleteClick = { deleteMenuCalls++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Delete").performClick()

        assertEquals(1, deleteMenuCalls)
    }

    @Test
    fun deleteConfirmationRequiresExplicitConfirmation() {
        var confirmed = 0
        composeTestRule.setContent {
            MaterialTheme {
                RecipeDeleteConfirmationDialog(
                    recipeName = "Toast",
                    isDeleting = false,
                    errorMessage = null,
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Delete recipe?").assertIsDisplayed()

        assertEquals(0, confirmed)
        composeTestRule.onNodeWithText("Delete").performClick()
        assertEquals(1, confirmed)
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
    fun ratingControlUsesTheGlobalDisplayAndOpensOneRatingDialog() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeActionControls(
                    actions = RecipeActionUiState(),
                    globalRating = 4.6666667,
                    onRatingSelected = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Your rating").assertCountEquals(0)
        composeTestRule.onNodeWithText("4.7 / 5").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open rating dialog").performClick()
        composeTestRule.onNodeWithText("Your rating").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Rate 1 out of 5 stars").assertIsDisplayed()
    }
}
