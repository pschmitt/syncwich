package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.NoteReferenceInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeCategoryInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeNoteInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeStepInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTagInputDto
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEditorDraftTest {

    @Test
    fun `blank name is rejected before a save request can be built`() {
        val draft = RecipeEditorDraft(description = "A tasty dinner")

        assertEquals("Enter a recipe name", draft.validationError())
    }

    @Test
    fun `a new recipe sends only the minimal create body`() {
        val draft = RecipeEditorDraft(name = "  Weeknight Tacos  ")

        assertNull(draft.validationError())
        assertEquals(CreateRecipeDto(name = "Weeknight Tacos"), draft.toCreateRequest())
    }

    @Test
    fun `an edit trims text fields, drops blank list rows, and preserves untouched fields`() {
        val base =
            RecipeInputDto(
                id = "recipe-1",
                userId = "user-1",
                slug = "weeknight-tacos",
                image = null,
                rating = 4.0,
            )
        val draft =
            RecipeEditorDraft(
                name = "  Weeknight Tacos  ",
                description = "  Fast and easy  ",
                recipeYield = "  4 servings  ",
                prepTime = "  10 minutes  ",
                cookTime = "  15 minutes  ",
                totalTime = "  25 minutes  ",
                ingredients = listOf("  1 lb ground beef  ", "  ", ""),
                instructions = listOf("  Brown the beef  ", ""),
                existingSlug = "weeknight-tacos",
                baseInput = base,
            )

        assertNull(draft.validationError())
        val request = draft.toUpdateRequest()

        assertEquals("Weeknight Tacos", request.name)
        assertEquals("Fast and easy", request.description)
        assertEquals("4 servings", request.recipeYield)
        assertEquals("10 minutes", request.prepTime)
        assertEquals("15 minutes", request.cookTime)
        assertEquals("25 minutes", request.totalTime)
        assertEquals(
            listOf(
                RecipeIngredientInputDto(
                    display = "1 lb ground beef",
                    note = "1 lb ground beef",
                    originalText = "1 lb ground beef",
                )
            ),
            request.recipeIngredient,
        )
        assertEquals(
            listOf(RecipeStepInputDto(text = "Brown the beef")),
            request.recipeInstructions,
        )
        // Fields the editor doesn't expose survive untouched from the cached recipe.
        assertEquals("recipe-1", request.id)
        assertEquals("user-1", request.userId)
        assertEquals(4.0, request.rating)
    }

    @Test
    fun `categories tags and tools are editable while matching server metadata is preserved`() {
        val base =
            RecipeInputDto(
                recipeCategory =
                    listOf(RecipeCategoryInputDto(id = "cat-1", name = "Dinner", slug = "dinner")),
                tags = listOf(RecipeTagInputDto(id = "tag-1", name = "Quick", slug = "quick")),
                tools = listOf(JsonPrimitive("Pan")),
            )
        val request =
            RecipeEditorDraft(
                    name = "Soup",
                    categories = "Dinner, Vegetarian",
                    tags = "Quick, Weeknight",
                    tools = "Pan, Blender",
                    baseInput = base,
                )
                .toUpdateRequest()

        assertEquals(listOf("Dinner", "Vegetarian"), request.recipeCategory.map { it.name })
        assertEquals("cat-1", request.recipeCategory.first().id)
        assertEquals(listOf("Quick", "Weeknight"), request.tags.map { it.name })
        assertEquals("tag-1", request.tags.first().id)
        assertEquals(listOf("Pan", "Blender"), request.tools.mapNotNull(::toolDisplayName))
    }

    @Test
    fun `an edit draft loads cached ingredients and steps as plain text rows`() {
        val input =
            RecipeInputDto(
                name = "Saved Soup",
                description = "Keep warm",
                recipeCategory = listOf(RecipeCategoryInputDto(name = "Dinner", slug = "dinner")),
                tags = listOf(RecipeTagInputDto(name = "Quick", slug = "quick")),
                tools = listOf(JsonPrimitive("Pot")),
                recipeIngredient =
                    listOf(
                        RecipeIngredientInputDto(display = "2 cups broth", note = "2 cups broth")
                    ),
                recipeInstructions = listOf(RecipeStepInputDto(text = "Simmer for 20 minutes")),
            )

        val draft = RecipeEditorDraft.from(input, slug = "saved-soup")

        assertEquals("Saved Soup", draft.name)
        assertEquals("Keep warm", draft.description)
        assertEquals("Dinner", draft.categories)
        assertEquals("Quick", draft.tags)
        assertEquals("Pot", draft.tools)
        assertEquals(listOf("2 cups broth"), draft.ingredients)
        assertEquals(listOf("Simmer for 20 minutes"), draft.instructions)
        assertEquals("saved-soup", draft.existingSlug)
    }

    @Test
    fun `removing the only ingredient or step row clears it instead of leaving an empty list`() {
        val draft = RecipeEditorDraft(ingredients = listOf("Salt"), instructions = listOf("Mix"))

        assertEquals(listOf(""), draft.withIngredientRemoved(0).ingredients)
        assertEquals(listOf(""), draft.withInstructionRemoved(0).instructions)
    }

    @Test
    fun `moving an instruction preserves the complete step content`() {
        val first = "First step\n\n![Image](https://example.test/first.webp)"
        val second = "Second step with **formatting**"
        val draft = RecipeEditorDraft(instructions = listOf(first, second))

        assertEquals(listOf(second, first), draft.withInstructionMoved(0, 1).instructions)
        assertEquals(draft, draft.withInstructionMoved(-1, 1))
    }

    @Test
    fun `markdown image insertion preserves existing content and cover selection`() {
        val draft = RecipeEditorDraft(description = "Intro", instructions = listOf("Step"))

        assertEquals(
            "Intro\n\n![Image](content://photo/1)",
            draft.withDescriptionImage("content://photo/1").description,
        )
        assertEquals(
            "Step\n\n![Image](content://photo/2)",
            draft.withInstructionImage(0, "content://photo/2").instructions.single(),
        )
        assertEquals("content://photo/3", draft.withCoverImage("content://photo/3").coverImageUri)
        assertEquals(true, draft.withoutCoverImage().removeCoverImage)
    }

    @Test
    fun `a new note gets a client-generated referenceId a step can link to before saving`() {
        val draft = RecipeEditorDraft().withNoteAdded()
        val note = draft.notes.single()

        assertTrue(note.referenceId.isNotBlank())

        val linked = draft.withStepNoteLinkToggled(0, note.referenceId)
        assertEquals(setOf(note.referenceId), linked.normalizedInstructionNoteReferences()[0])

        val unlinked = linked.withStepNoteLinkToggled(0, note.referenceId)
        assertEquals(emptySet<String>(), unlinked.normalizedInstructionNoteReferences()[0])
    }

    @Test
    fun `removing a note also unlinks it from every step`() {
        val draft = RecipeEditorDraft().withNoteAdded()
        val referenceId = draft.notes.single().referenceId
        val linked = draft.withStepNoteLinkToggled(0, referenceId)

        val afterRemoval = linked.withNoteRemoved(0)

        assertEquals(emptyList<RecipeEditorNote>(), afterRemoval.notes)
        assertEquals(emptySet<String>(), afterRemoval.normalizedInstructionNoteReferences()[0])
    }

    @Test
    fun `adding, removing, and moving steps keeps note links aligned by index`() {
        val draft =
            RecipeEditorDraft(instructions = listOf("Step 1", "Step 2")).withNoteAdded()
        val referenceId = draft.notes.single().referenceId
        val linked = draft.withStepNoteLinkToggled(1, referenceId)

        val withThirdStep = linked.withInstructionAdded()
        assertEquals(3, withThirdStep.normalizedInstructionNoteReferences().size)
        assertEquals(setOf(referenceId), withThirdStep.normalizedInstructionNoteReferences()[1])
        assertEquals(emptySet<String>(), withThirdStep.normalizedInstructionNoteReferences()[2])

        val moved = linked.withInstructionMoved(1, 0)
        assertEquals(listOf("Step 2", "Step 1"), moved.instructions)
        assertEquals(setOf(referenceId), moved.normalizedInstructionNoteReferences()[0])

        val afterRemovingStepZero = linked.withInstructionRemoved(0)
        assertEquals(listOf("Step 2"), afterRemovingStepZero.instructions)
        assertEquals(setOf(referenceId), afterRemovingStepZero.normalizedInstructionNoteReferences()[0])
    }

    @Test
    fun `saving sends notes and step note links, dropping blank notes and their links`() {
        val draft = RecipeEditorDraft(name = "Soup", instructions = listOf("Simmer", "Serve"))
        val withNote = draft.withNoteAdded()
        val referenceId = withNote.notes.single().referenceId
        val titled =
            withNote
                .withNoteTitleChanged(0, "  Tip  ")
                .withNoteTextChanged(0, "  Use a fan oven.  ")
                .withStepNoteLinkToggled(0, referenceId)
                .withNoteAdded() // a second, still-blank note that should be dropped on save

        val request = titled.toUpdateRequest()

        assertEquals(
            listOf(RecipeNoteInputDto(title = "Tip", text = "Use a fan oven.", referenceId = referenceId)),
            request.notes,
        )
        assertEquals(
            listOf(NoteReferenceInputDto(referenceId)),
            request.recipeInstructions[0].noteReferences,
        )
        assertEquals(emptyList<NoteReferenceInputDto>(), request.recipeInstructions[1].noteReferences)
    }

    @Test
    fun `saving drops a step's link to a note that was removed`() {
        val draft = RecipeEditorDraft(name = "Soup", instructions = listOf("Simmer")).withNoteAdded()
        val referenceId = draft.notes.single().referenceId
        // Simulate a stale link surviving without going through withNoteRemoved (which already
        // cleans this up) by dropping the note directly - toUpdateRequest must filter it too.
        val linkedThenRemoved = draft.withStepNoteLinkToggled(0, referenceId).copy(notes = emptyList())

        val request = linkedThenRemoved.toUpdateRequest()

        assertEquals(emptyList<RecipeNoteInputDto>(), request.notes)
        assertEquals(emptyList<NoteReferenceInputDto>(), request.recipeInstructions[0].noteReferences)
    }

    @Test
    fun `an edit draft loads existing notes and each step's linked notes`() {
        val referenceId = "9c6e1e2a-1111-4a1a-8888-abc123456789"
        val input =
            RecipeInputDto(
                name = "Braised Beef",
                recipeInstructions =
                    listOf(
                        RecipeStepInputDto(
                            text = "Preheat the oven.",
                            noteReferences = listOf(NoteReferenceInputDto(referenceId)),
                        ),
                        RecipeStepInputDto(text = "Sear the beef."),
                    ),
                notes = listOf(RecipeNoteInputDto(title = "Oven tip", text = "Use the fan setting.", referenceId = referenceId)),
            )

        val draft = RecipeEditorDraft.from(input, slug = "braised-beef")

        assertEquals(
            listOf(RecipeEditorNote(referenceId, "Oven tip", "Use the fan setting.")),
            draft.notes,
        )
        assertEquals(setOf(referenceId), draft.normalizedInstructionNoteReferences()[0])
        assertEquals(emptySet<String>(), draft.normalizedInstructionNoteReferences()[1])
    }
}
