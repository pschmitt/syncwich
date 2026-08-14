package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
import dev.pschmitt.syncwich.ui.common.RefreshState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeDetailDecodeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `decodes the cached recipe detail shape`() {
        val recipe =
            decodeRecipeDetail(
                json,
                """
                {"id":"recipe-1","slug":"recipe-one","name":"Recipe One",
                 "recipeIngredient":[],"recipeInstructions":[]}
                """
                    .trimIndent(),
            )

        assertEquals(
            RecipeDetailDto(id = "recipe-1", slug = "recipe-one", name = "Recipe One"),
            recipe,
        )
    }

    @Test
    fun `malformed cached JSON stays unavailable instead of escaping the Flow`() {
        assertNull(decodeRecipeDetail(json, "not-json"))
    }

    @Test
    fun `cached detail stays visible while a best-effort refresh is active`() {
        val cachedRecipe = RecipeDetailDto(id = "recipe-1", slug = "recipe-one", name = "Cached")

        val state =
            recipeDetailUiState(
                recipe = cachedRecipe,
                actions = RecipeActionUiState(),
                serverUrl = "https://mealie.example",
                refresh = RefreshState(isRefreshing = true),
            )

        assertEquals(cachedRecipe, (state as RecipeDetailUiState.Loaded).recipe)
    }
}
