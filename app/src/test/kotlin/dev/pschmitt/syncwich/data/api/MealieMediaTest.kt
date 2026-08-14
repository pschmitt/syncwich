package dev.pschmitt.syncwich.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the "no image" sentinel gotcha found live against a v3.22.0 Mealie instance while building
 * SW-3: a recipe with no cover image has `image: "no image"` in both `/api/recipes` and
 * `/api/recipes/{slug}` responses - not `null` like the rest of the DTOs' optional string fields.
 */
class MealieMediaTest {

    @Test
    fun `builds the min-original cover image URL when an image version is present`() {
        assertEquals(
            "https://mealie.example.com/api/media/recipes/abc-123/images/min-original.webp?v=130",
            recipeImageUrl("https://mealie.example.com", "abc-123", "130"),
        )
    }

    @Test
    fun `changes to the Mealie image version produce a new Coil cache key`() {
        val first = recipeImageUrl("https://mealie.example.com", "abc-123", "130")
        val second = recipeImageUrl("https://mealie.example.com", "abc-123", "131")

        assertEquals(false, first == second)
    }

    @Test
    fun `normalizes a trailing slash without changing the cache key`() {
        assertEquals(
            recipeImageUrl("https://mealie.example.com", "abc-123", "130"),
            recipeImageUrl("https://mealie.example.com/", "abc-123", "130"),
        )
    }

    @Test
    fun `returns null for the literal 'no image' sentinel`() {
        assertNull(recipeImageUrl("https://mealie.example.com", "abc-123", "no image"))
    }

    @Test
    fun `returns null for a null or blank image field`() {
        assertNull(recipeImageUrl("https://mealie.example.com", "abc-123", null))
        assertNull(recipeImageUrl("https://mealie.example.com", "abc-123", ""))
    }

    @Test
    fun `returns null when the server URL is blank`() {
        assertNull(recipeImageUrl("", "abc-123", "130"))
    }
}
