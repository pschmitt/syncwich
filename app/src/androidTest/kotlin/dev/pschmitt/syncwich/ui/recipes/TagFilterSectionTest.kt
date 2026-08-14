package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagFilterSectionTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun collapsedTagFilterCanBeExpandedWithoutLosingAllTags() {
        composeTestRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            MaterialTheme {
                TagFilterSection(
                    tags =
                        listOf(
                            TagEntity("tag-1", "Quick", "quick"),
                            TagEntity("tag-2", "Vegetarian", "vegetarian"),
                        ),
                    selectedTagId = null,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onSelected = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Show tags (2)").performClick()
        composeTestRule.onNodeWithText("Quick").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vegetarian").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide tags").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag("recipe-search-tag-icon", useUnmergedTree = true)
            .assertCountEquals(2)
    }
}
