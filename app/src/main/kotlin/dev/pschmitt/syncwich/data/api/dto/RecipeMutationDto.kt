package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** The minimal body accepted by Mealie's `POST /api/recipes` route. */
@Serializable data class CreateRecipeDto(val name: String)

/** Confirmed against Mealie's live OpenAPI `ScrapeRecipe` schema. */
@Serializable
data class ScrapeRecipeDto(
    val includeTags: Boolean = true,
    val includeCategories: Boolean = true,
    val url: String,
)

/**
 * The editable recipe payload accepted by Mealie v3.22.0's single-recipe PUT/PATCH routes.
 *
 * This intentionally models the complete top-level `Recipe-Input` envelope while leaving the
 * server-owned and less common nested values as JSON elements. An editor can grow the nested models
 * without making the repository silently discard fields from an existing recipe.
 */
@Serializable
data class RecipeInputDto(
    val id: String? = null,
    val userId: String? = null,
    val householdId: String? = null,
    val groupId: String? = null,
    val name: String? = null,
    val slug: String = "",
    val image: JsonElement? = null,
    val recipeServings: Double = 0.0,
    val recipeYieldQuantity: Double = 0.0,
    val recipeYield: String? = null,
    val totalTime: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val performTime: String? = null,
    val description: String = "",
    val recipeCategory: List<RecipeCategoryInputDto> = emptyList(),
    val tags: List<RecipeTagInputDto> = emptyList(),
    val tools: List<JsonElement> = emptyList(),
    val rating: Double? = null,
    val orgURL: String? = null,
    val dateAdded: String? = null,
    val dateUpdated: String? = null,
    val createdAt: String? = null,
    @SerialName("update_at") val updateAt: String? = null,
    val lastMade: String? = null,
    val recipeIngredient: List<RecipeIngredientInputDto> = emptyList(),
    val recipeInstructions: List<RecipeStepInputDto> = emptyList(),
    val nutrition: JsonElement? = null,
    val settings: JsonElement? = null,
    val assets: List<JsonElement> = emptyList(),
    val notes: List<RecipeNoteInputDto> = emptyList(),
    val extras: JsonElement? = null,
    val comments: List<JsonElement> = emptyList(),
)

/**
 * `referenceId` (a UUID4 string) lets a [RecipeStepInputDto] link to this note via
 * [NoteReferenceInputDto]. `title`/`text` are `@EncodeDefault`-forced: Mealie's schema requires
 * both present even when blank, but this app's shared [kotlinx.serialization.json.Json] has
 * `encodeDefaults = false`, which would otherwise drop a blank one from the request entirely and
 * fail server-side validation with a 422 ("Field required").
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RecipeNoteInputDto(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val title: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val text: String = "",
    val referenceId: String? = null,
)

@Serializable
data class RecipeCategoryInputDto(
    val id: String? = null,
    val groupId: String? = null,
    val name: String,
    val slug: String,
)

@Serializable
data class RecipeTagInputDto(
    val id: String? = null,
    val groupId: String? = null,
    val name: String,
    val slug: String,
)

@Serializable
data class RecipeIngredientInputDto(
    val quantity: Double? = null,
    val unit: JsonElement? = null,
    val food: JsonElement? = null,
    val referencedRecipe: JsonElement? = null,
    val note: String? = null,
    val display: String = "",
    val title: String? = null,
    val originalText: String? = null,
    val referenceId: String? = null,
)

@Serializable
data class RecipeStepInputDto(
    val id: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val text: String,
    val ingredientReferences: List<JsonElement> = emptyList(),
    val noteReferences: List<NoteReferenceInputDto> = emptyList(),
)

@Serializable data class NoteReferenceInputDto(val referenceId: String? = null)
