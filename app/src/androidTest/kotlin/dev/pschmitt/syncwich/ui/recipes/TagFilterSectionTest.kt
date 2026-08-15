package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagFilterSectionTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun filterButtonInvokesTheBottomSheetAction() {
        var clicked = false

        composeTestRule.setContent {
            MaterialTheme {
                RecipeFilterButton(selectedFilterCount = 0, onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithContentDescription("Filters").performClick()
        assertTrue(clicked)
    }

    @Test
    fun filterButtonSitsBesideTheSearchFieldWithoutAVisibleLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeSearchControls(
                    searchQuery = "",
                    onSearchQueryChange = {},
                    filtersAvailable = true,
                    selectedFilterCount = 1,
                    onFilterClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Search recipes").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Filters, 1 active")
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Filters").assertCountEquals(0)
    }

    @Test
    fun filterSheetShowsCategoriesAndTagsAndSupportsClearing() {
        var selectedTagId: String? = null
        var cleared = false
        var dismissed = false

        composeTestRule.setContent {
            MaterialTheme {
                RecipeFilterSheet(
                    categories = listOf(CategoryEntity("category-1", "Dinner", "dinner")),
                    tags =
                        listOf(
                            TagEntity("tag-1", "Quick", "quick"),
                            TagEntity("tag-2", "Vegetarian", "vegetarian"),
                        ),
                    selectedCategoryId = "category-1",
                    selectedTagId = null,
                    onCategorySelected = {},
                    onTagSelected = { selectedTagId = it },
                    onClearFilters = { cleared = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Filter recipes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tags").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick").performClick()
        composeTestRule.onNodeWithText("Clear filters").performClick()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals("tag-1", selectedTagId)
        assertTrue(cleared)
        assertTrue(dismissed)
        composeTestRule
            .onAllNodesWithTag("recipe-search-tag-icon", useUnmergedTree = true)
            .assertCountEquals(2)
        composeTestRule
            .onAllNodesWithTag("recipe-search-category-icon", useUnmergedTree = true)
            .assertCountEquals(1)
    }
}
