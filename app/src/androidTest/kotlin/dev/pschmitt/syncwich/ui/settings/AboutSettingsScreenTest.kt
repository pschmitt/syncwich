package dev.pschmitt.syncwich.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutSettingsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun aboutScreenShowsBuildAndProjectInformationOffline() {
        composeTestRule.setContent { SyncwichTheme { AboutSettingsScreen(onBack = {}) } }

        composeTestRule.onNodeWithText("Syncwich").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("about-app-icon-background", useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Syncwich app icon").assertExists()
        composeTestRule.onNodeWithText("Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub repository").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sponsor the project").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy policy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug build").assertExists()
    }

    @Test
    fun aboutScreenShowsLibrariesAndLicensesOffline() {
        composeTestRule.setContent { SyncwichTheme { AboutSettingsScreen(onBack = {}) } }

        composeTestRule.onNodeWithTag("about-settings-list").performScrollToIndex(2)
        composeTestRule.onNodeWithText("Libraries").assertIsDisplayed()
        composeTestRule.onNodeWithText("AndroidX").assertExists()
        composeTestRule.onNodeWithText("Jetpack Compose").assertExists()
        composeTestRule.onNodeWithText("Multiplatform Markdown Renderer").assertExists()
        composeTestRule.onAllNodesWithText("Apache License 2.0").assertCountEquals(15)
        composeTestRule.onNodeWithText("AndroidX").assertHasClickAction()
    }

    @Test
    fun buildRowUnlocksDeveloperModeAfterSevenTaps() {
        var taps = 0
        composeTestRule.setContent {
            SyncwichTheme {
                AboutSettingsScreen(
                    onBack = {},
                    onBuildTap = { taps++ },
                )
            }
        }

        repeat(7) { composeTestRule.onNodeWithText("Build").performClick() }

        assertEquals(7, taps)
        composeTestRule
            .onAllNodesWithText("Developer mode enabled", substring = true)
            .assertCountEquals(0)
    }
}
