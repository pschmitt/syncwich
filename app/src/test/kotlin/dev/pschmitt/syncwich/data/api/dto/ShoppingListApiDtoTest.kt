package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests pinning our shopping-list DTOs to the *actual* Mealie v3.22.0 response shapes
 * confirmed against a live instance during SW-5 (see TODO.md's verification notes) - not the public
 * docs, which AGENTS.md flags as a source of drift. Fixtures below are shape-accurate but use
 * placeholder content rather than real household data.
 */
class ShoppingListApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes a paginated shopping list response`() {
        val body =
            """
            {"page":1,"per_page":50,"total":1,"total_pages":1,"items":[
              {"name":"Rezeptideen","extras":{},"createdAt":"2026-05-26T21:12:17.254774Z",
               "updatedAt":"2026-05-26T21:12:44.303777Z","groupId":"a205f72f","userId":"c39a9bd3",
               "id":"5aa52dd6-dcad-4d85-867d-549fdeb4a8c8","householdId":"ba99bf35",
               "recipeReferences":[],"labelSettings":[]}
            ],"next":null,"previous":null}
            """
                .trimIndent()

        val decoded =
            json.decodeFromString(
                PagedResponseDto.serializer(ShoppingListSummaryDto.serializer()),
                body,
            )

        assertEquals(1, decoded.total)
        val list = decoded.items.single()
        assertEquals("5aa52dd6-dcad-4d85-867d-549fdeb4a8c8", list.id)
        assertEquals("Rezeptideen", list.name)
        assertEquals("2026-05-26T21:12:44.303777Z", list.updatedAt)
    }

    @Test
    fun `decodes a shopping list detail response with items`() {
        val body =
            """
            {"name":"Rezeptideen","extras":{},"createdAt":"2026-05-26T21:12:17.254774Z",
             "updatedAt":"2026-05-26T21:12:44.303777Z","groupId":"a205f72f","userId":"c39a9bd3",
             "id":"5aa52dd6-dcad-4d85-867d-549fdeb4a8c8",
             "listItems":[
               {"quantity":0.0,"unit":null,
                "food":{"id":"ef2caf38","name":"Risotto","pluralName":null,"description":"",
                  "extras":{},"labelId":null,"aliases":[],"householdsWithIngredientFood":[],
                  "label":null,"createdAt":"2026-05-26T21:12:24.815598Z",
                  "updatedAt":"2026-05-26T21:12:24.815600Z"},
                "referencedRecipe":null,"note":"","display":"Risotto",
                "shoppingListId":"5aa52dd6-dcad-4d85-867d-549fdeb4a8c8","checked":false,
                "position":0,"foodId":"ef2caf38","labelId":null,"unitId":null,"extras":{},
                "id":"076e8f95-301e-4088-b5f6-3bbef5565778","groupId":"a205f72f",
                "householdId":"ba99bf35","label":null,"recipeReferences":[],
                "createdAt":"2026-05-26T21:12:44.301224Z","updatedAt":"2026-05-26T21:12:44.301229Z"}
             ],
             "householdId":"ba99bf35","recipeReferences":[],"labelSettings":[]}
            """
                .trimIndent()

        val decoded = json.decodeFromString<ShoppingListDto>(body)

        assertEquals("Rezeptideen", decoded.name)
        val item = decoded.listItems.single()
        assertEquals("076e8f95-301e-4088-b5f6-3bbef5565778", item.id)
        assertEquals("5aa52dd6-dcad-4d85-867d-549fdeb4a8c8", item.shoppingListId)
        assertEquals("Risotto", item.display)
        assertEquals("", item.note)
        assertFalse(item.checked)
        assertEquals(0, item.position)
    }

    @Test
    fun `decodes a checked item`() {
        val body =
            """
            {"quantity":1.0,"unit":null,"food":null,"referencedRecipe":null,"note":"ripe ones",
             "display":"2 apples","shoppingListId":"5aa52dd6","checked":true,"position":1,
             "foodId":null,"labelId":null,"unitId":null,"extras":{},"id":"item-2",
             "groupId":"a205f72f","householdId":"ba99bf35","label":null,"recipeReferences":[],
             "createdAt":"2026-05-26T21:12:44Z","updatedAt":"2026-05-26T21:12:44Z"}
            """
                .trimIndent()

        val decoded = json.decodeFromString<ShoppingListItemDto>(body)

        assertTrue(decoded.checked)
        assertEquals("2 apples", decoded.display)
        assertEquals("ripe ones", decoded.note)
        assertEquals("5aa52dd6", decoded.shoppingListId)
    }
}
