package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeImageViewerTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun viewerDisplaysImageMetadataAndAccessiblePagerState() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeImageViewer(
                    images =
                        listOf(
                            RecipeViewerImage(
                                url = "https://example.test/cover.jpg",
                                title = "Recipe One",
                                sourceLabel = "Recipe cover",
                                altText = "Finished dish",
                            )
                        ),
                    initialPage = 0,
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Recipe One").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recipe cover").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finished dish").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Recipe One, Recipe cover, image 1 of 1")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Not zoomed; swipe left or right to change images",
                )
            )
    }

    @Test
    fun doubleTapTogglesZoomStateWithoutRemovingImageAccessibility() {
        composeTestRule.setContent {
            MaterialTheme {
                RecipeImageViewer(
                    images =
                        listOf(
                            RecipeViewerImage(
                                url = "https://example.test/cover.jpg",
                                title = "Recipe One",
                                sourceLabel = "Recipe cover",
                            )
                        ),
                    initialPage = 0,
                    onDismiss = {},
                )
            }
        }

        val image =
            composeTestRule.onNodeWithContentDescription("Recipe One, Recipe cover, image 1 of 1")
        image.performTouchInput { doubleClick(center) }
        image.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Zoomed in; pan with one finger",
            )
        )
    }
}
