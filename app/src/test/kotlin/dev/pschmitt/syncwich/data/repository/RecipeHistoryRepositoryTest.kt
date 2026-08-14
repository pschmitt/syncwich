package dev.pschmitt.syncwich.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeHistoryRepositoryTest {

    @Test
    fun `opening a recipe moves it to the front without duplicates`() {
        assertEquals(
            listOf("new", "old", "other"),
            updatedRecipeHistory(listOf("old", "new", "other", "new"), "new"),
        )
    }

    @Test
    fun `history is bounded and keeps the newest open`() {
        assertEquals(
            listOf("new", "one", "two"),
            updatedRecipeHistory(listOf("one", "two", "three"), "new", limit = 3),
        )
    }

    @Test
    fun `blank ids do not create history entries and existing entries are normalized`() {
        assertEquals(
            listOf("one", "two"),
            updatedRecipeHistory(listOf(" one ", "", "two", "one"), " "),
        )
    }
}
