package dev.pschmitt.syncwich.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutSettingsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun aboutScreenShowsBuildAndProjectInformationOffline() {
        composeTestRule.setContent {
            SyncwichTheme { AboutSettingsScreen(onBack = {}) }
        }

        composeTestRule.onNodeWithText("Syncwich").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub repository").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy policy").assertIsDisplayed()
    }
}
