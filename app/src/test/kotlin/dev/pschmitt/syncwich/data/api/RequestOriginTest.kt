package dev.pschmitt.syncwich.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestOriginTest {

    @Test
    fun `retrofit placeholder is rewritten with a path-prefixed server exactly once`() {
        val request = "http://mealie.invalid/api/recipes/recipe-1".toHttpUrl()
        val configured = "https://recipes.example/mealie".toHttpUrl()

        assertEquals(
            "https://recipes.example/mealie/api/recipes/recipe-1",
            rewriteDynamicBaseUrl(request, configured).toString(),
        )
    }

    @Test
    fun `absolute same-origin media URL is preserved`() {
        val request = "https://recipes.example/mealie/api/media/recipe.webp".toHttpUrl()
        val configured = "https://recipes.example/mealie".toHttpUrl()

        assertEquals(request, rewriteDynamicBaseUrl(request, configured))
    }

    @Test
    fun `external image URL is not rewritten`() {
        val request = "https://images.example/recipe.webp".toHttpUrl()
        val configured = "https://recipes.example/mealie".toHttpUrl()

        assertEquals(request, rewriteDynamicBaseUrl(request, configured))
    }

    @Test
    fun `authorization is limited to the configured origin`() {
        val configured = "https://recipes.example/mealie".toHttpUrl()

        assertTrue(
            shouldAttachMealieAuth(
                "https://recipes.example/mealie/api/recipes".toHttpUrl(),
                configured,
            )
        )
        assertFalse(
            shouldAttachMealieAuth("https://images.example/recipe.webp".toHttpUrl(), configured)
        )
        assertFalse(
            shouldAttachMealieAuth(
                "http://recipes.example/mealie/api/recipes".toHttpUrl(),
                configured,
            )
        )
    }
}
