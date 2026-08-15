package dev.pschmitt.syncwich.ui.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeGridPerformanceTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun largeCachedGridScrollsToTheEndWithoutNetworkWork() {
        val recipes = (0 until 120).map(::recipe)

        composeTestRule.setContent {
            MaterialTheme { RecipeGrid(recipes = recipes, serverUrl = "", onRecipeClick = {}) }
        }

        composeTestRule.onNodeWithTag("recipes-grid").performScrollToIndex(recipes.lastIndex)
        composeTestRule.onNodeWithText("Recipe 119").assertIsDisplayed()
    }

    private fun recipe(index: Int) =
        RecipeSummaryEntity(
            id = "recipe-$index",
            slug = "recipe-$index",
            name = "Recipe $index",
            description = "",
            image = null,
            rating = null,
            prepTime = null,
            totalTime = null,
            dateAdded = null,
            lastMade = null,
        )
}
