package dev.pschmitt.syncwich.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingModeControlsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun modeControlsHaveEqualHeightWhenPasswordLabelWraps() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.width(312.dp)) {
                    OnboardingModeControls(
                        passwordMode = false,
                        onPasswordModeChange = {},
                    )
                }
            }
        }

        val tokenBounds =
            composeTestRule.onNodeWithTag("onboarding-mode-token").getUnclippedBoundsInRoot()
        val passwordBounds =
            composeTestRule.onNodeWithTag("onboarding-mode-password").getUnclippedBoundsInRoot()

        assertEquals(
            tokenBounds.bottom - tokenBounds.top,
            passwordBounds.bottom - passwordBounds.top,
        )
        assertEquals(tokenBounds.top, passwordBounds.top)
    }
}
