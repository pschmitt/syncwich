package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Regression test pinning [CookbookDto] to the *actual* Mealie v3.22.0 response shape confirmed
 * live during SW-6 (`GET /api/households/cookbooks`, verification account) - not the public docs.
 * The fixture keeps `queryFilterString`/`queryFilter`/`household` (unmodeled, dropped by
 * `ignoreUnknownKeys`) to prove real extra fields don't break decoding.
 */
class CookbookApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes a paginated cookbook list response`() {
        val body =
            """
            {"page":1,"per_page":50,"total":2,"total_pages":1,"items":[
              {"name":"Chinese Nom Nom","description":"","slug":"chinese-nom-nom","position":1,
               "public":false,
               "queryFilterString":"recipe_category.id IN [\"51912eb6-80bf-4cbb-8fad-bde0bad6535d\"]",
               "groupId":"a205f72f-dbc1-4cf1-88b4-02cc034abba8",
               "householdId":"ba99bf35-d3d3-4532-abb9-d792b5e24248",
               "id":"40401aa9-4f96-4073-871b-96cff037df31",
               "queryFilter":{"parts":[{"leftParenthesis":null,"rightParenthesis":null,
                 "logicalOperator":null,"attributeName":"recipe_category.id",
                 "relationalOperator":"IN","value":["51912eb6-80bf-4cbb-8fad-bde0bad6535d"]}]},
               "household":{"id":"ba99bf35-d3d3-4532-abb9-d792b5e24248","name":"Bergmann-Schmitt"}},
              {"name":"Backbuch","description":"","slug":"backbuch","position":2,"public":false,
               "queryFilterString":"recipe_category.id IN [\"bed83f2f-545a-44d2-a4e0-d7e56ca073cf\"]",
               "groupId":"a205f72f-dbc1-4cf1-88b4-02cc034abba8",
               "householdId":"ba99bf35-d3d3-4532-abb9-d792b5e24248",
               "id":"0bd4c744-4669-4ae0-aee9-016bfe0ff742",
               "queryFilter":{"parts":[]},
               "household":{"id":"ba99bf35-d3d3-4532-abb9-d792b5e24248","name":"Bergmann-Schmitt"}}
            ],"next":null,"previous":null}
            """
                .trimIndent()

        val decoded =
            json.decodeFromString(PagedResponseDto.serializer(CookbookDto.serializer()), body)

        assertEquals(2, decoded.total)
        assertEquals(2, decoded.items.size)

        val cookbook = decoded.items.first()
        assertEquals("40401aa9-4f96-4073-871b-96cff037df31", cookbook.id)
        assertEquals("Chinese Nom Nom", cookbook.name)
        assertEquals("chinese-nom-nom", cookbook.slug)
        assertEquals(1, cookbook.position)
        assertFalse(cookbook.public)
    }
}
