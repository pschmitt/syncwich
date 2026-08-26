package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test pinning [FoodDto] to the *actual* Mealie v3.24.0 response shape confirmed live
 * during SW-137 (`GET /api/foods`, read-only, "Mealie (AI Agent)" verification account) - not just
 * the public `/openapi.json` schema. The fixture keeps `extras`/`labelId`/`aliases`/
 * `householdsWithIngredientFood`/`label`/`createdAt`/`updatedAt` (unmodeled, dropped by
 * `ignoreUnknownKeys`) to prove real extra fields don't break decoding.
 */
class FoodApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes a paginated food list response`() {
        val body =
            """
            {"page":1,"per_page":3,"total":189,"total_pages":63,"items":[
              {"id":"3f50c488-996e-4a47-96aa-41835282874e",
               "name":"⚪ Ssam Mu (pickled radish)",
               "pluralName":"Ssam Mu (pickled radish)","description":"","extras":{},
               "labelId":null,"aliases":[],"householdsWithIngredientFood":[],"label":null,
               "createdAt":"2026-08-26T15:55:05.376667Z","updatedAt":"2026-08-26T15:55:05.376669Z"},
              {"id":"3f611caf-becd-4405-bd1c-8577e37d0d91",
               "name":"🍜 Naengmyeon (Korean cold noodles)",
               "pluralName":"Naengmyeon (Korean cold noodles)","description":"","extras":{},
               "labelId":null,"aliases":[],"householdsWithIngredientFood":[],"label":null,
               "createdAt":"2026-08-26T15:55:05.288992Z","updatedAt":"2026-08-26T16:00:44.136274Z"}
            ],"next":"/foods?page=2&perPage=3","previous":null}
            """
                .trimIndent()

        val decoded = json.decodeFromString(PagedResponseDto.serializer(FoodDto.serializer()), body)

        assertEquals(189, decoded.total)
        assertEquals(2, decoded.items.size)

        val food = decoded.items.first()
        assertEquals("3f50c488-996e-4a47-96aa-41835282874e", food.id)
        assertEquals("⚪ Ssam Mu (pickled radish)", food.name)
        assertEquals("Ssam Mu (pickled radish)", food.pluralName)
        assertEquals("", food.description)
    }

    @Test
    fun `decodes a food with no plural name`() {
        val body = """{"id":"abc","name":"Salt","description":"Table salt"}"""

        val decoded = json.decodeFromString(FoodDto.serializer(), body)

        assertEquals("abc", decoded.id)
        assertEquals("Salt", decoded.name)
        assertEquals(null, decoded.pluralName)
        assertEquals("Table salt", decoded.description)
    }
}
