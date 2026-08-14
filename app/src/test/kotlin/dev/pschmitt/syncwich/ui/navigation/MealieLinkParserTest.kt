package dev.pschmitt.syncwich.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealieLinkParserTest {

    @Test
    fun `parses the public recipe route`() {
        assertEquals(
            MealieLinkTarget.Recipe("gochujang-schweinebauch"),
            parseMealieLink("https://nom.example/g/home/r/gochujang-schweinebauch"),
        )
    }

    @Test
    fun `parses cookbook links and percent encoded slugs`() {
        assertEquals(
            MealieLinkTarget.Cookbook("Korean Nom Nom"),
            parseMealieLink("https://nom.example/g/home/c/Korean%20Nom%20Nom"),
        )
    }

    @Test
    fun `rejects unrelated or non web links`() {
        assertNull(parseMealieLink("https://nom.example/g/home"))
        assertNull(parseMealieLink("javascript:alert(1)"))
    }

    @Test
    fun `routes shared external web URLs to recipe import`() {
        assertEquals(
            "https://example.com/article/kimchi",
            parseSharedRecipeText("https://example.com/article/kimchi"),
        )
        assertEquals(
            "https://example.com/article/kimchi",
            parseSharedRecipeText("Kimchi recipe\nhttps://example.com/article/kimchi"),
        )
    }

    @Test
    fun `does not intercept recognized Mealie links`() {
        assertNull(parseSharedRecipeText("https://nom.example/g/home/r/kimchi"))
        assertNull(parseSharedRecipeText("Kimchi recipe\nhttps://nom.example/g/home/r/kimchi"))
    }

    @Test
    fun `ignores shared text that is not a web URL`() {
        assertNull(parseSharedRecipeText("Kimchi recipe"))
    }
}
