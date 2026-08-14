package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeStepControlsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun checkingAStepReportsCompletion() {
        var completed = false
        composeTestRule.setContent {
            SyncwichTheme {
                InstructionRow(
                    number = 1,
                    instruction = RecipeInstructionDto(text = "Stir the sauce."),
                    imageReferences = emptyList(),
                    onCompletedChange = { completed = it },
                    onImageClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Mark step 1 complete")
            .assertIsDisplayed()
            .performClick()

        assertEquals(true, completed)
    }

    @Test
    fun transientFontControlsChangeOnlyTheFullScreenLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                StepFontSizeControls(
                    fontScale = 1f,
                    onDecrease = {},
                    onIncrease = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("step-font-size-controls").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Increase step text size").performClick()
    }
}
