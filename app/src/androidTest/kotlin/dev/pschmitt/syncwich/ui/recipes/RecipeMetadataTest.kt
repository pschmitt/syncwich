package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeMetadataTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun tagsAndCookbooksAreDisplayedAndClickable() {
        var openedTag: String? = null
        var openedCookbook: String? = null
        composeTestRule.setContent {
            MaterialTheme {
                RecipeMetadataCard(
                    tags = listOf(OrganizerDto(id = "tag-1", name = "Quick", slug = "quick")),
                    cookbooks =
                        listOf(
                            CookbookEntity(
                                id = "book-1",
                                name = "Weeknights",
                                slug = "weeknights",
                                description = "",
                                position = 0,
                            )
                        ),
                    onOpenTag = { openedTag = it },
                    onOpenCookbook = { openedCookbook = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Recipe details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick").performClick()
        composeTestRule.onNodeWithText("Weeknights").performClick()

        assertEquals("tag-1", openedTag)
        assertEquals("book-1", openedCookbook)
    }
}
