package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeStepInputDto

/**
 * In-memory editor state kept intact when a save fails, including while the device is offline -
 * mirrors [dev.pschmitt.syncwich.ui.cookbooks.CookbookEditorDraft]. Ingredients/instructions are
 * edited as plain text lines; Mealie's structured `unit`/`food`/`ingredientReferences` fields are
 * left untouched by this bounded editor (see [RecipeIngredientInputDto]/[RecipeStepInputDto]).
 */
data class RecipeEditorDraft(
    val name: String = "",
    val description: String = "",
    val recipeYield: String = "",
    val prepTime: String = "",
    val cookTime: String = "",
    val totalTime: String = "",
    val ingredients: List<String> = listOf(""),
    val instructions: List<String> = listOf(""),
    val coverImageUri: String? = null,
    val removeCoverImage: Boolean = false,
    val existingSlug: String? = null,
    // Every field this bounded editor doesn't expose (id, image, category/tags, nutrition,
    // settings, assets, notes, extras, comments, tools) is preserved from the cached recipe so
    // saving an edit never silently discards it - see AGENTS.md's offline-cache rule.
    val baseInput: RecipeInputDto? = null,
) {

    fun validationError(): String? =
        when {
            name.isBlank() -> "Enter a recipe name"
            name.trim().length > MAX_NAME_LENGTH ->
                "Recipe names must be $MAX_NAME_LENGTH characters or fewer"
            else -> null
        }

    fun toCreateRequest(): CreateRecipeDto = CreateRecipeDto(name = name.trim())

    fun toUpdateRequest(): RecipeInputDto {
        val trimmedIngredients = ingredients.map(String::trim).filter(String::isNotEmpty)
        val trimmedInstructions = instructions.map(String::trim).filter(String::isNotEmpty)
        val base = baseInput ?: RecipeInputDto()
        return base.copy(
            name = name.trim(),
            description = description.trim(),
            recipeYield = recipeYield.trim().ifBlank { null },
            prepTime = prepTime.trim().ifBlank { null },
            cookTime = cookTime.trim().ifBlank { null },
            totalTime = totalTime.trim().ifBlank { null },
            recipeIngredient =
                trimmedIngredients.map { text ->
                    RecipeIngredientInputDto(display = text, note = text, originalText = text)
                },
            recipeInstructions =
                trimmedInstructions.map { text -> RecipeStepInputDto(text = text) },
        )
    }

    fun withIngredientChanged(index: Int, value: String): RecipeEditorDraft =
        copy(ingredients = ingredients.toMutableList().apply { set(index, value) })

    fun withIngredientAdded(): RecipeEditorDraft = copy(ingredients = ingredients + "")

    fun withIngredientRemoved(index: Int): RecipeEditorDraft =
        if (ingredients.size <= 1) copy(ingredients = listOf(""))
        else copy(ingredients = ingredients.toMutableList().apply { removeAt(index) })

    fun withInstructionChanged(index: Int, value: String): RecipeEditorDraft =
        copy(instructions = instructions.toMutableList().apply { set(index, value) })

    fun withInstructionAdded(): RecipeEditorDraft = copy(instructions = instructions + "")

    fun withInstructionRemoved(index: Int): RecipeEditorDraft =
        if (instructions.size <= 1) copy(instructions = listOf(""))
        else copy(instructions = instructions.toMutableList().apply { removeAt(index) })

    fun withInstructionMoved(from: Int, to: Int): RecipeEditorDraft {
        if (from !in instructions.indices || to !in instructions.indices || from == to) return this
        return copy(
            instructions =
                instructions.toMutableList().also { items -> items.add(to, items.removeAt(from)) }
        )
    }

    fun withDescriptionImage(uri: String): RecipeEditorDraft =
        copy(description = appendMarkdownImage(description, uri))

    fun withInstructionImage(index: Int, uri: String): RecipeEditorDraft =
        withInstructionChanged(index, appendMarkdownImage(instructions[index], uri))

    fun withCoverImage(uri: String): RecipeEditorDraft =
        copy(coverImageUri = uri, removeCoverImage = false)

    fun withoutCoverImage(): RecipeEditorDraft = copy(coverImageUri = null, removeCoverImage = true)

    companion object {
        private const val MAX_NAME_LENGTH = 200

        /** Builds an edit draft from one cached recipe's decoded `Recipe-Input` envelope. */
        fun from(input: RecipeInputDto, slug: String): RecipeEditorDraft =
            RecipeEditorDraft(
                name = input.name.orEmpty(),
                description = input.description,
                recipeYield = input.recipeYield.orEmpty(),
                prepTime = input.prepTime.orEmpty(),
                cookTime = input.cookTime.orEmpty(),
                totalTime = input.totalTime.orEmpty(),
                ingredients =
                    input.recipeIngredient
                        .map { it.display.takeIf(String::isNotBlank) ?: it.note.orEmpty() }
                        .ifEmpty { listOf("") },
                instructions = input.recipeInstructions.map { it.text }.ifEmpty { listOf("") },
                existingSlug = slug,
                baseInput = input,
            )
    }
}

internal fun appendMarkdownImage(content: String, uri: String): String =
    listOf(content.trimEnd(), "![Image]($uri)").filter(String::isNotBlank).joinToString("\n\n")
