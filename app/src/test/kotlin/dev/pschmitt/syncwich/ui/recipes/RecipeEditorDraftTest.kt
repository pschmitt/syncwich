package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.RecipeCategoryInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeStepInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTagInputDto
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
