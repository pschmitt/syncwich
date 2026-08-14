package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.ui.common.MarkdownEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeEditorImageCaptureTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun markdownEditorExposesGalleryAndCameraImageActions() {
        var galleryClicks = 0
        var cameraClicks = 0
        composeTestRule.setContent {
            MaterialTheme {
                MarkdownEditor(
                    value = "",
                    onValueChange = {},
                    label = "Description",
                    enabled = true,
                    onAddImage = { galleryClicks++ },
                    onCaptureImage = { cameraClicks++ },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add image").assertIsDisplayed().performClick()
        composeTestRule
            .onNodeWithContentDescription("Take photo")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, galleryClicks)
        assertEquals(1, cameraClicks)
    }

    @Test
    fun cameraOutputUsesThePrivateFileProvider() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val uri = createRecipeEditorCameraUri(context)

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertTrue(uri.path.orEmpty().contains("recipe_editor_camera"))
    }
}
