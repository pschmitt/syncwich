package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto
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
                """.trimIndent(),
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
}
