package dev.pschmitt.syncwich.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RichTextEditorTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun formattingToolbarUpdatesMarkdownAndShowsLivePreview() {
        var value = ""
        composeTestRule.setContent {
            MaterialTheme {
                MarkdownEditor(
                    value = value,
                    onValueChange = { value = it },
                    label = "Description",
                    enabled = true,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Bold").performClick()
        composeTestRule.onNodeWithText("Live preview").assertIsDisplayed()
        assertTrue(value.contains("**bold text**"))
    }
}
