package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMutationDtoTest {
    private val json = Json { explicitNulls = false }

    @Test
    fun `create recipe serializes the required name only`() {
        assertEquals("{\"name\":\"Toast\"}", json.encodeToString(CreateRecipeDto("Toast")))
    }

    @Test
    fun `recipe input uses the schema's update_at property`() {
        val encoded = json.encodeToString(RecipeInputDto(name = "Toast", updateAt = "2026-08-14"))

        assertTrue(encoded.contains("\"name\":\"Toast\""))
        assertTrue(encoded.contains("\"update_at\":\"2026-08-14\""))
        assertTrue(!encoded.contains("\"updateAt\""))
    }

    @Test
    fun `clearing a rating keeps an explicit JSON null`() {
        val encoded = json.encodeToString(UserRatingUpdateDto.forRating(null))

        assertEquals("{\"rating\":null}", encoded)
    }
}
