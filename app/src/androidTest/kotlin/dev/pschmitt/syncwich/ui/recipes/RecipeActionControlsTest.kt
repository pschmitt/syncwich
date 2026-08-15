package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material.icons.filled.Star
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
                    globalRating = 4.6666667,
                    onRatingSelected = { rating = it },
                    onDismiss = {},
                    onFavoriteClick = { favorite = it },
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                    onShareClick = {},
                    onOpenBrowserClick = {},
                    onDeleteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Favorite").performClick()
        composeTestRule.onNodeWithText("Rate recipe").performClick()
        composeTestRule.onNodeWithText("Overall rating: 4.7 / 5").assertIsDisplayed()
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
                RecipePendingSyncBanner(actions = RecipeActionUiState(madeThisPending = true))
            }
        }

        composeTestRule.onNodeWithText("Saved offline; sync pending").assertIsDisplayed()
    }

    @Test
    fun ratingMenuItemUsesTheGlobalDisplayAndOpensOneRatingDialog() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeOverflowMenu(
                    expanded = true,
                    actions = RecipeActionUiState(),
                    globalRating = 4.6666667,
                    onRatingSelected = {},
                    onDismiss = {},
                    onFavoriteClick = {},
                    onMadeThisClick = {},
                    onOpenTimelineClick = {},
                    onShareClick = {},
                    onOpenBrowserClick = {},
                    onDeleteClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Your rating").assertCountEquals(0)
        composeTestRule.onNodeWithText("Rate recipe").performClick()
        composeTestRule.onNodeWithText("Overall rating: 4.7 / 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your rating").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Rate 1 out of 5 stars").assertIsDisplayed()
    }

    @Test
    fun unratedRecipeKeepsRatingInTheOverflowMenuWithoutAnAverageBadge() {
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
                    onDeleteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Rate recipe").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("No ratings yet").assertCountEquals(0)
    }

    @Test
    fun sectionHeadersRelyOnCardSpacingInsteadOfSeparators() {
        composeTestRule.setContent {
            MaterialTheme {
                SectionHeader(
                    icon = androidx.compose.material.icons.Icons.Filled.Star,
                    title = "Ingredients",
                )
            }
        }

        composeTestRule.onNodeWithText("Ingredients").assertIsDisplayed()
    }
}
