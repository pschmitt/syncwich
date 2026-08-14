package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests pinning [MealPlanEntryDto] to the `ReadPlanEntry`/`PlanEntryPagination` schemas
 * confirmed against a live v3.22.0 Mealie instance's `/openapi.json` during SW-4 (see the task's
 * verification notes in TODO.md - the live household had no meal-plan entries to sample a populated
 * response from, so this fixture is schema-accurate rather than a captured live payload, unlike
 * [RecipeApiDtoTest]'s fixtures).
 */
class MealPlanApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes an empty paginated meal plan response`() {
        val body = """{"page":1,"per_page":50,"total":0,"total_pages":0,"items":[],"next":null,"previous":null}"""

        val decoded =
            json.decodeFromString(PagedResponseDto.serializer(MealPlanEntryDto.serializer()), body)

        assertEquals(0, decoded.total)
        assertTrue(decoded.items.isEmpty())
        assertNull(decoded.next)
    }

    @Test
    fun `decodes a meal plan entry with an embedded recipe`() {
        val body =
            """
            {"page":1,"per_page":50,"total":1,"total_pages":1,"items":[
              {"date":"2026-08-11","entryType":"dinner","title":"","text":"","recipeId":"448f77cd-cc5b-4099-9074-86ae78ee348b",
               "id":42,"groupId":"a205f72f-dbc1-4cf1-88b4-02cc034abba8","userId":"bcb7a427-b4c5-48d5-aee1-b190f888303e",
               "householdId":"ba99bf35-d3d3-4532-abb9-d792b5e24248",
               "recipe":{"id":"448f77cd-cc5b-4099-9074-86ae78ee348b","userId":"e652a6c8","householdId":"ba99bf35",
                 "groupId":"a205f72f","name":"Test Cake","slug":"test-cake","image":"130","rating":5.0,
                 "prepTime":null,"totalTime":null,"dateAdded":"2026-06-13","lastMade":null,
                 "recipeCategory":[],"tags":[]}}
            ],"next":null,"previous":null}
            """
                .trimIndent()

        val decoded =
            json.decodeFromString(PagedResponseDto.serializer(MealPlanEntryDto.serializer()), body)

        val entry = decoded.items.single()
        assertEquals(42, entry.id)
        assertEquals("2026-08-11", entry.date)
        assertEquals("dinner", entry.entryType)
        assertEquals("448f77cd-cc5b-4099-9074-86ae78ee348b", entry.recipeId)
        assertEquals("test-cake", entry.recipe?.slug)
        assertEquals("Test Cake", entry.recipe?.name)
    }

    @Test
    fun `decodes a meal plan entry with no recipe as a freeform note`() {
        val body =
            """
            {"date":"2026-08-12","entryType":"snack","title":"Leftover night","text":"Whatever's in the fridge",
             "recipeId":null,"id":7,"groupId":"a205f72f-dbc1-4cf1-88b4-02cc034abba8",
             "userId":"bcb7a427-b4c5-48d5-aee1-b190f888303e","householdId":"ba99bf35-d3d3-4532-abb9-d792b5e24248",
             "recipe":null}
            """
                .trimIndent()

        val entry = json.decodeFromString<MealPlanEntryDto>(body)

        assertEquals("snack", entry.entryType)
        assertEquals("Leftover night", entry.title)
        assertNull(entry.recipeId)
        assertNull(entry.recipe)
    }
}
