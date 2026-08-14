package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeEditorDecodeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `decodes a cached recipe detail as the editable Recipe-Input envelope`() {
        val input =
            decodeRecipeInput(
                json,
                """
                {"id":"recipe-1","slug":"recipe-one","name":"Recipe One",
                 "recipeIngredient":[],"recipeInstructions":[]}
                """.trimIndent(),
            )

        assertEquals(RecipeInputDto(id = "recipe-1", slug = "recipe-one", name = "Recipe One"), input)
    }

    @Test
    fun `malformed cached JSON stays unavailable instead of escaping the Flow`() {
        assertNull(decodeRecipeInput(json, "not-json"))
    }
}
