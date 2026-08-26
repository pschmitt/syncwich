package dev.pschmitt.syncwich.ui.foods

import dev.pschmitt.syncwich.data.api.dto.FoodMutationDto
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodEditorDraftTest {

    @Test
    fun `blank name is rejected before a save request can be built`() {
        val draft = FoodEditorDraft(description = "Some description")

        assertEquals("Enter a food name", draft.validationError())
    }

    @Test
    fun `valid draft trims editable text and blanks a blank plural name`() {
        val draft =
            FoodEditorDraft(name = "  Flour  ", pluralName = "   ", description = "  Baking  ")

        assertNull(draft.validationError())
        assertEquals(
            FoodMutationDto(name = "Flour", pluralName = null, description = "Baking"),
            draft.toRequest(),
        )
    }

    @Test
    fun `an edit draft loads all cached food fields`() {
        val draft =
            FoodEditorDraft.from(
                FoodEntity(
                    id = "food-1",
                    name = "Tomato",
                    pluralName = "Tomatoes",
                    description = "Fresh or canned",
                )
            )

        assertEquals("Tomato", draft.name)
        assertEquals("Tomatoes", draft.pluralName)
        assertEquals("Fresh or canned", draft.description)
    }

    @Test
    fun `a seeded draft pre-fills only the name`() {
        val draft = FoodEditorDraft.seeded("2 cups all-purpose flour")

        assertEquals("2 cups all-purpose flour", draft.name)
        assertEquals("", draft.pluralName)
        assertEquals("", draft.description)
    }
}
