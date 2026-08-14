package dev.pschmitt.syncwich.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
            SyncwichTheme {
                SettingsScreen(onBack = {}, onCategoryClick = {})
            }
        }

        composeTestRule.onAllNodesWithText("About").assertCountEquals(1)
    }
}
