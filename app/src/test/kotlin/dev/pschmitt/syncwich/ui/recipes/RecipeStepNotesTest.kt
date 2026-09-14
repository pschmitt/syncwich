package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.NoteReferenceDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInstructionDto
import dev.pschmitt.syncwich.data.api.dto.RecipeNoteDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeStepNotesTest {

    private val linkedNote = RecipeNoteDto(title = "Tip", text = "Use a fan oven.", referenceId = "ref-1")
    private val otherNote = RecipeNoteDto(title = "Other", text = "Unrelated.", referenceId = "ref-2")

    @Test
    fun `resolves notes referenced by a step`() {
        val instruction =
            RecipeInstructionDto(
                text = "Preheat the oven.",
                noteReferences = listOf(NoteReferenceDto(referenceId = "ref-1")),
            )

        assertEquals(listOf(linkedNote), linkedNotesFor(instruction, listOf(linkedNote, otherNote)))
    }

    @Test
    fun `returns nothing when a step has no note references`() {
        val instruction = RecipeInstructionDto(text = "Preheat the oven.")

        assertTrue(linkedNotesFor(instruction, listOf(linkedNote)).isEmpty())
    }

    @Test
    fun `ignores note references that no longer match a recipe note`() {
        val instruction =
            RecipeInstructionDto(
                text = "Preheat the oven.",
                noteReferences = listOf(NoteReferenceDto(referenceId = "stale-ref")),
            )

        assertTrue(linkedNotesFor(instruction, listOf(linkedNote)).isEmpty())
    }
}
