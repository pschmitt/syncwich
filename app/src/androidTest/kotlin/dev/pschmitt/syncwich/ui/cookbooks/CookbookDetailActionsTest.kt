package dev.pschmitt.syncwich.ui.cookbooks

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CookbookDetailActionsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun deleteMenuItemInvokesDeleteRequest() {
        var menuDeleteCalls = 0
        composeTestRule.setContent {
            MaterialTheme {
                CookbookOverflowMenu(
                    expanded = true,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteClick = { menuDeleteCalls++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Delete").performClick()

        assertEquals(1, menuDeleteCalls)
    }

    @Test
    fun deleteConfirmationRequiresExplicitConfirmation() {
        var confirmed = 0
        composeTestRule.setContent {
            MaterialTheme {
                CookbookDeleteConfirmationDialog(
                    cookbookName = "Weeknight meals",
                    isDeleting = false,
                    errorMessage = null,
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Delete cookbook?").assertIsDisplayed()

        assertEquals(0, confirmed)
        composeTestRule.onNodeWithText("Delete").performClick()
        assertEquals(1, confirmed)
    }
}
