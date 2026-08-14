package dev.pschmitt.syncwich.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialAuditComponentsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun searchFieldHasOneAccessibleLabelAndClearAction() {
        var value = "pasta"
        composeTestRule.setContent {
            MaterialTheme {
                SearchField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "Search recipes",
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Search recipes").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", value)
    }

    @Test
    fun refreshErrorBannerIsVisibleAndRetryIsActionable() {
        var retryCount = 0
        composeTestRule.setContent {
            MaterialTheme {
                RefreshErrorBanner(
                    errorMessage = "Couldn't refresh. Showing saved data.",
                    onRetry = { retryCount++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Couldn't refresh. Showing saved data.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun centeredContentCapsWideFormsForReadableLineLength() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).height(200.dp)) {
                    CenteredContent(maxWidth = 160.dp) {
                        Box(Modifier.testTag("content").fillMaxSize())
                    }
                }
            }
        }

        val bounds = composeTestRule.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertEquals(160.dp, bounds.right - bounds.left)
    }
}
