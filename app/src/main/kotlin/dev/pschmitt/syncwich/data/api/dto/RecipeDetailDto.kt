package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `/api/recipes/{slug}` full detail response shape, confirmed field-by-field against a live v3.22.0
 * Mealie instance (see SW-2's verification notes). [dev.pschmitt.syncwich.data.db] stores this
 * endpoint's raw JSON body verbatim in `RecipeDetailEntity.detailJson` rather than normalizing it
 * into Room columns/join tables (AGENTS.md's offline-cache section) - this DTO is the shared shape
 * a consumer (e.g. the recipe detail screen) should use to decode that stored JSON with
 * `Json.decodeFromString<RecipeDetailDto>(...)`.
 *
 * `unit`/`tools`/`comments`/`extras` here are left as loose [JsonElement]/[JsonObject] rather than
 * fully modeled - every recipe checked on the verification instance had them null/empty, so their
 * populated shape wasn't confirmed live; decode them further only once actually seen non-empty.
 * `food` on [RecipeIngredientDto] WAS re-confirmed populated on a live re-check for SW-142 (this
 * server's ingredient lines are now food-referenced almost universally - unlike SW-2's original
 * verification, which predates this server's library being curated with structured foods), so it's
 * fully modeled (id/name only - the rest of `IngredientFood-Output`'s shape is irrelevant here).
 */
@Serializable
data class RecipeDetailDto(
    val id: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    val image: String? = null,
    val recipeYield: String? = null,
    val recipeServings: Double? = null,
    val totalTime: String? = null,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val performTime: String? = null,
    val rating: Double? = null,
    val orgURL: String? = null,
    val dateAdded: String? = null,
    val dateUpdated: String? = null,
    val lastMade: String? = null,
    val recipeCategory: List<OrganizerDto> = emptyList(),
    val tags: List<OrganizerDto> = emptyList(),
    val tools: List<JsonElement> = emptyList(),
    val recipeIngredient: List<RecipeIngredientDto> = emptyList(),
    val recipeInstructions: List<RecipeInstructionDto> = emptyList(),
    val nutrition: RecipeNutritionDto? = null,
    val settings: RecipeSettingsDto? = null,
    val assets: List<RecipeAssetDto> = emptyList(),
    val notes: List<RecipeNoteDto> = emptyList(),
    val comments: List<JsonElement> = emptyList(),
    val extras: JsonObject? = null,
)

@Serializable
data class RecipeIngredientDto(
    val quantity: Double? = null,
    val unit: JsonElement? = null,
    val food: RecipeIngredientFoodDto? = null,
    val note: String? = null,
    val display: String? = null,
    val title: String? = null,
    val originalText: String? = null,
    val referenceId: String? = null,
)

/** Only the fields SW-142's recipe-by-food filter needs from `IngredientFood-Output`. */
@Serializable data class RecipeIngredientFoodDto(val id: String, val name: String? = null)

@Serializable
data class RecipeInstructionDto(
    val id: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val text: String,
)

@Serializable
data class RecipeNutritionDto(
    val calories: String? = null,
    val carbohydrateContent: String? = null,
    val proteinContent: String? = null,
    val fatContent: String? = null,
    val saturatedFatContent: String? = null,
    val unsaturatedFatContent: String? = null,
    val transFatContent: String? = null,
    val sugarContent: String? = null,
    val fiberContent: String? = null,
    val sodiumContent: String? = null,
    val cholesterolContent: String? = null,
)

@Serializable
data class RecipeSettingsDto(
    val public: Boolean = false,
    val showNutrition: Boolean = false,
    val showAssets: Boolean = false,
    val landscapeView: Boolean = false,
    val disableComments: Boolean = false,
    val locked: Boolean = false,
)

@Serializable
data class RecipeAssetDto(
    val name: String? = null,
    val icon: String? = null,
    val fileName: String? = null,
)

@Serializable data class RecipeNoteDto(val title: String? = null, val text: String? = null)
