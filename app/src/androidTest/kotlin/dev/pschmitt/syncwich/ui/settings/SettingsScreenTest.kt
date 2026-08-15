package dev.pschmitt.syncwich.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.ui.theme.SyncwichTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun aboutIsShownAsOneHeaderlessSettingsItem() {
        composeTestRule.setContent {
            SyncwichTheme { SettingsScreen(onBack = {}, onCategoryClick = {}) }
        }

        composeTestRule.onAllNodesWithText("About").assertCountEquals(1)
    }

    @Test
    fun personalizationExposesNavigationBarAsItsOwnSettingsEntry() {
        composeTestRule.setContent {
            SyncwichTheme { SettingsScreen(onBack = {}, onCategoryClick = {}) }
        }

        composeTestRule.onNodeWithTag("settings-list").performScrollToNode(hasText("Navigation bar"))
        composeTestRule.onAllNodesWithText("Navigation bar").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Theme, text, and recipe display").assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("Choose destinations and their order")
            .assertCountEquals(1)
    }
}
