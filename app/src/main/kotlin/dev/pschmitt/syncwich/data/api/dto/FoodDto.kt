package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Mealie's structured ingredient-food catalog item (`/api/foods`), confirmed against a live v3.24.0
 * instance's `/openapi.json` (`IngredientFood-Output`). This is a separate concept from a recipe's
 * own `recipeIngredient` entries (see [RecipeDetailDto]'s kdoc) - those store freeform notes on
 * this server, with no `food` reference wired up, so this catalog is edited standalone rather than
 * resolved automatically from a recipe. `aliases`/`extras`/`labelId`/
 * `householdsWithIngredientFood` are left unmodeled since this app only edits name/pluralName/
 * description.
 */
@Serializable
data class FoodDto(
    val id: String,
    val name: String,
    val pluralName: String? = null,
    val description: String = "",
)

/**
 * Shared body shape for `POST /api/foods` and `PUT /api/foods/{id}` (both `CreateIngredientFood`).
 */
@Serializable
data class FoodMutationDto(
    val name: String,
    val pluralName: String? = null,
    val description: String = "",
)
