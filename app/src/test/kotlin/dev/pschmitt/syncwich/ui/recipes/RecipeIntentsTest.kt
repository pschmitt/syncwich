package dev.pschmitt.syncwich.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeIntentsTest {

    @Test
    fun `recipe web url preserves a path prefixed server`() {
        assertEquals(
            "https://recipes.example/mealie/g/home/r/tofu-stew",
            recipeWebUrl("https://recipes.example/mealie", "tofu-stew"),
        )
    }

    @Test
    fun `blank slug cannot produce an external action url`() {
        assertNull(recipeWebUrl("https://recipes.example", " "))
    }
}
