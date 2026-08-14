package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests pinning our DTOs to the *actual* Mealie v3.22.0 response shapes confirmed
 * against a live instance during SW-2 (see the task's verification notes) - not the public docs,
 * which AGENTS.md flags as a source of drift a prior sibling app got burned by. Fixtures below are
 * shape-accurate (field names, nesting, snake_case pagination keys) but use placeholder content
 * rather than real recipe data.
 */
class RecipeApiDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `decodes a paginated recipe list response`() {
        val body =
            """
            {"page":1,"per_page":3,"total":63,"total_pages":21,"items":[
              {"id":"448f77cd-cc5b-4099-9074-86ae78ee348b","userId":"e652a6c8","householdId":"ba99bf35",
               "groupId":"a205f72f","name":"Test Cake","slug":"test-cake","image":"130",
               "recipeServings":0.0,"totalTime":null,"prepTime":null,"cookTime":null,"performTime":null,
               "description":"","recipeCategory":[{"id":"bed83f2f","groupId":"a205f72f","name":"Baking","slug":"baking"}],
               "tags":[],"tools":[],"rating":5.0,"orgURL":null,"dateAdded":"2026-06-13",
               "dateUpdated":"2026-06-13T19:08:14.903448+00:00","lastMade":"2026-06-13T21:59:59+00:00"}
            ],"next":"/recipes?page=2&perPage=3","previous":null}
            """
                .trimIndent()

        val decoded =
            json.decodeFromString(PagedResponseDto.serializer(RecipeSummaryDto.serializer()), body)

        assertEquals(1, decoded.page)
        assertEquals(3, decoded.perPage)
        assertEquals(63, decoded.total)
        assertEquals(21, decoded.totalPages)
        assertEquals("/recipes?page=2&perPage=3", decoded.next)
        assertNull(decoded.previous)

        val recipe = decoded.items.single()
        assertEquals("test-cake", recipe.slug)
        assertEquals("Test Cake", recipe.name)
        assertEquals("130", recipe.image)
        assertEquals(5.0, recipe.rating)
        assertEquals("Baking", recipe.recipeCategory.single().name)
        assertEquals(emptyList<OrganizerDto>(), recipe.tags)
    }

    @Test
    fun `decodes a paginated organizer (category or tag) response`() {
        val body =
            """
            {"page":1,"per_page":5,"total":7,"total_pages":2,"items":[
              {"id":"51912eb6","groupId":"a205f72f","name":"Chinese","slug":"chinese"},
              {"id":"fda325c1","groupId":"a205f72f","name":"Dessert","slug":"dessert"}
            ],"next":"/categories?page=2&perPage=5","previous":null}
            """
                .trimIndent()

        val decoded =
            json.decodeFromString(PagedResponseDto.serializer(OrganizerDto.serializer()), body)

        assertEquals(7, decoded.total)
        assertEquals(2, decoded.items.size)
        assertEquals("Chinese", decoded.items[0].name)
        assertEquals("chinese", decoded.items[0].slug)
    }

    @Test
    fun `decodes a full recipe detail response`() {
        val body =
            """
            {"id":"448f77cd","userId":"e652a6c8","householdId":"ba99bf35","groupId":"a205f72f",
             "name":"Test Cake","slug":"test-cake","image":"130","recipeServings":0.0,
             "totalTime":null,"prepTime":null,"cookTime":null,"performTime":null,"description":"",
             "recipeCategory":[{"id":"bed83f2f","groupId":"a205f72f","name":"Baking","slug":"baking"}],
             "tags":[],"rating":5.0,"orgURL":null,"dateAdded":"2026-06-13",
             "dateUpdated":"2026-06-13T19:08:14.903448Z","lastMade":"2026-06-13T21:59:59Z",
             "recipeIngredient":[
               {"quantity":0.0,"unit":null,"food":null,"referencedRecipe":null,"note":"130 g flour",
                "display":"130 g flour","title":"","originalText":null,"referenceId":"16972d38"}
             ],
             "recipeInstructions":[
               {"id":"27d14501","title":"","summary":"","text":"Preheat oven to 180C.","ingredientReferences":[]}
             ],
             "nutrition":{"calories":"0","carbohydrateContent":null,"cholesterolContent":null,"fatContent":null,
               "fiberContent":null,"proteinContent":null,"saturatedFatContent":null,"sodiumContent":null,
               "sugarContent":null,"transFatContent":null,"unsaturatedFatContent":null},
             "settings":{"public":false,"showNutrition":true,"showAssets":true,"landscapeView":false,
               "disableComments":false,"locked":false},
             "assets":[{"name":"image.jpg","icon":"mdi-file-image","fileName":"image-jpg.jpg"}],
             "notes":[],"extras":{},"comments":[]}
            """
                .trimIndent()

        val decoded = json.decodeFromString<RecipeDetailDto>(body)

        assertEquals("test-cake", decoded.slug)
        assertEquals(1, decoded.recipeIngredient.size)
        assertEquals("130 g flour", decoded.recipeIngredient.single().note)
        assertEquals(1, decoded.recipeInstructions.size)
        assertEquals("Preheat oven to 180C.", decoded.recipeInstructions.single().text)
        assertEquals("0", decoded.nutrition?.calories)
        assertEquals(false, decoded.settings?.public)
        assertEquals("image.jpg", decoded.assets.single().name)
    }
}
