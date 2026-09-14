package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.NoteReferenceInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeCategoryInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeIngredientInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeNoteInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeStepInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTagInputDto
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * In-memory editor state kept intact when a save fails, including while the device is offline -
 * mirrors [dev.pschmitt.syncwich.ui.cookbooks.CookbookEditorDraft]. Ingredients/instructions are
 * edited as plain text lines; Mealie's structured `unit`/`food`/`ingredientReferences` fields are
 * left untouched by this bounded editor (see [RecipeIngredientInputDto]/[RecipeStepInputDto]).
 * Notes are the exception: they're fully editable (title/text/add/remove), and each step can link
 * to any of them (Mealie v3.26.0's step note-linking) via [instructionNoteReferences], a list kept
 * index-aligned with [instructions] by every mutator below.
 */
data class RecipeEditorDraft(
    val name: String = "",
    val description: String = "",
    val recipeYield: String = "",
    val prepTime: String = "",
    val cookTime: String = "",
    val totalTime: String = "",
    val categories: String = "",
    val tags: String = "",
    val tools: String = "",
    val ingredients: List<String> = listOf(""),
    val instructions: List<String> = listOf(""),
    val notes: List<RecipeEditorNote> = emptyList(),
    // Index-aligned with [instructions]: instructionNoteReferences[i] is the set of note
    // referenceIds linked to instructions[i]. Use [normalizedInstructionNoteReferences] rather
    // than indexing this directly - it may be shorter than [instructions] (e.g. the default `[]`).
    val instructionNoteReferences: List<Set<String>> = emptyList(),
    val coverImageUri: String? = null,
    val removeCoverImage: Boolean = false,
    val existingSlug: String? = null,
    // Every field this bounded editor doesn't expose (id, image, category/tags, nutrition,
    // settings, assets, extras, comments, tools) is preserved from the cached recipe so saving an
    // edit never silently discards it - see AGENTS.md's offline-cache rule.
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
        val nonBlankInstructionIndexes = instructions.indices.filter { instructions[it].isNotBlank() }
        val normalizedNoteReferences = normalizedInstructionNoteReferences()
        val trimmedNotes = notes.filter { it.title.isNotBlank() || it.text.isNotBlank() }
        val survivingNoteIds = trimmedNotes.map(RecipeEditorNote::referenceId).toSet()
        val base = baseInput ?: RecipeInputDto()
        return base.copy(
            name = name.trim(),
            description = description.trim(),
            recipeYield = recipeYield.trim().ifBlank { null },
            prepTime = prepTime.trim().ifBlank { null },
            cookTime = cookTime.trim().ifBlank { null },
            totalTime = totalTime.trim().ifBlank { null },
            recipeCategory = editableCategories(categories, base.recipeCategory),
            tags = editableTags(tags, base.tags),
            tools = editableTools(tools, base.tools),
            recipeIngredient =
                trimmedIngredients.map { text ->
                    RecipeIngredientInputDto(display = text, note = text, originalText = text)
                },
            recipeInstructions =
                nonBlankInstructionIndexes.map { index ->
                    RecipeStepInputDto(
                        text = instructions[index].trim(),
                        noteReferences =
                            normalizedNoteReferences[index]
                                .filter { it in survivingNoteIds }
                                .map { referenceId -> NoteReferenceInputDto(referenceId) },
                    )
                },
            notes =
                trimmedNotes.map { note ->
                    RecipeNoteInputDto(
                        title = note.title.trim(),
                        text = note.text.trim(),
                        referenceId = note.referenceId,
                    )
                },
        )
    }

    /**
     * [instructionNoteReferences] padded/truncated to [instructions]' size - safe to index
     * positionally, unlike the raw field (which may be shorter, e.g. the default `[]`).
     */
    fun normalizedInstructionNoteReferences(): List<Set<String>> =
        List(instructions.size) { instructionNoteReferences.getOrElse(it) { emptySet() } }

    fun withIngredientChanged(index: Int, value: String): RecipeEditorDraft =
        copy(ingredients = ingredients.toMutableList().apply { set(index, value) })

    fun withIngredientAdded(): RecipeEditorDraft = copy(ingredients = ingredients + "")

    fun withIngredientRemoved(index: Int): RecipeEditorDraft =
        if (ingredients.size <= 1) copy(ingredients = listOf(""))
        else copy(ingredients = ingredients.toMutableList().apply { removeAt(index) })

    fun withInstructionChanged(index: Int, value: String): RecipeEditorDraft =
        copy(instructions = instructions.toMutableList().apply { set(index, value) })

    fun withInstructionAdded(): RecipeEditorDraft =
        copy(
            instructions = instructions + "",
            instructionNoteReferences = normalizedInstructionNoteReferences() + emptySet(),
        )

    fun withInstructionRemoved(index: Int): RecipeEditorDraft =
        if (instructions.size <= 1) {
            copy(instructions = listOf(""), instructionNoteReferences = listOf(emptySet()))
        } else {
            copy(
                instructions = instructions.toMutableList().apply { removeAt(index) },
                instructionNoteReferences =
                    normalizedInstructionNoteReferences().toMutableList().apply {
                        removeAt(index)
                    },
            )
        }

    fun withInstructionMoved(from: Int, to: Int): RecipeEditorDraft {
        if (from !in instructions.indices || to !in instructions.indices || from == to) return this
        return copy(
            instructions =
                instructions.toMutableList().also { items -> items.add(to, items.removeAt(from)) },
            instructionNoteReferences =
                normalizedInstructionNoteReferences().toMutableList().also { items ->
                    items.add(to, items.removeAt(from))
                },
        )
    }

    fun withDescriptionImage(uri: String): RecipeEditorDraft =
        copy(description = appendMarkdownImage(description, uri))

    fun withInstructionImage(index: Int, uri: String): RecipeEditorDraft =
        withInstructionChanged(index, appendMarkdownImage(instructions[index], uri))

    fun withStepNoteLinkToggled(stepIndex: Int, referenceId: String): RecipeEditorDraft {
        val updated =
            normalizedInstructionNoteReferences().toMutableList().apply {
                val current = this[stepIndex]
                this[stepIndex] =
                    if (referenceId in current) current - referenceId else current + referenceId
            }
        return copy(instructionNoteReferences = updated)
    }

    fun withNoteAdded(): RecipeEditorDraft =
        copy(notes = notes + RecipeEditorNote(referenceId = UUID.randomUUID().toString()))

    fun withNoteTitleChanged(index: Int, value: String): RecipeEditorDraft =
        copy(notes = notes.toMutableList().apply { set(index, this[index].copy(title = value)) })

    fun withNoteTextChanged(index: Int, value: String): RecipeEditorDraft =
        copy(notes = notes.toMutableList().apply { set(index, this[index].copy(text = value)) })

    fun withNoteRemoved(index: Int): RecipeEditorDraft {
        val removedId = notes[index].referenceId
        return copy(
            notes = notes.toMutableList().apply { removeAt(index) },
            instructionNoteReferences =
                normalizedInstructionNoteReferences().map { it - removedId },
        )
    }

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
                categories = input.recipeCategory.joinToString(", ") { it.name },
                tags = input.tags.joinToString(", ") { it.name },
                tools = input.tools.mapNotNull(::toolDisplayName).joinToString(", "),
                ingredients =
                    input.recipeIngredient
                        .map { it.display.takeIf(String::isNotBlank) ?: it.note.orEmpty() }
                        .ifEmpty { listOf("") },
                instructions =
                    input.recipeInstructions.map { it.text }.ifEmpty { listOf("") },
                notes =
                    input.notes.map { note ->
                        RecipeEditorNote(
                            referenceId = note.referenceId ?: UUID.randomUUID().toString(),
                            title = note.title,
                            text = note.text,
                        )
                    },
                instructionNoteReferences =
                    input.recipeInstructions
                        .map { step -> step.noteReferences.mapNotNull { it.referenceId }.toSet() }
                        .ifEmpty { listOf(emptySet()) },
                existingSlug = slug,
                baseInput = input,
            )
    }
}

/**
 * One recipe note as edited locally; `referenceId` is a UUID4, generated up front so a step can
 * link to a brand-new note before it's ever saved (Mealie accepts a client-supplied referenceId).
 */
data class RecipeEditorNote(val referenceId: String, val title: String = "", val text: String = "")

internal fun appendMarkdownImage(content: String, uri: String): String =
    listOf(content.trimEnd(), "![Image]($uri)").filter(String::isNotBlank).joinToString("\n\n")

internal fun parseEditorNames(value: String): List<String> =
    value.split(',', '\n').map(String::trim).filter(String::isNotBlank).distinctBy {
        it.lowercase(Locale.ROOT)
    }

private fun organizerSlug(name: String): String =
    name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')

internal fun editableCategories(
    value: String,
    previous: List<RecipeCategoryInputDto>,
): List<RecipeCategoryInputDto> =
    parseEditorNames(value).map { name ->
        val slug = organizerSlug(name)
        val old = previous.firstOrNull { it.name.equals(name, true) || it.slug == slug }
        RecipeCategoryInputDto(id = old?.id, groupId = old?.groupId, name = name, slug = slug)
    }

internal fun editableTags(
    value: String,
    previous: List<RecipeTagInputDto>,
): List<RecipeTagInputDto> =
    parseEditorNames(value).map { name ->
        val slug = organizerSlug(name)
        val old = previous.firstOrNull { it.name.equals(name, true) || it.slug == slug }
        RecipeTagInputDto(id = old?.id, groupId = old?.groupId, name = name, slug = slug)
    }

internal fun toolDisplayName(element: JsonElement): String? =
    when (element) {
        is JsonPrimitive -> element.contentOrNull?.takeIf(String::isNotBlank)
        is JsonObject ->
            listOf("name", "display", "title")
                .firstNotNullOfOrNull { key -> element[key]?.jsonPrimitive?.contentOrNull }
                ?.takeIf(String::isNotBlank)
        else -> null
    }

internal fun editableTools(value: String, previous: List<JsonElement>): List<JsonElement> =
    parseEditorNames(value).map { name ->
        previous.firstOrNull { toolDisplayName(it).equals(name, true) }
            ?: buildJsonObject { put("name", name) }
    }
