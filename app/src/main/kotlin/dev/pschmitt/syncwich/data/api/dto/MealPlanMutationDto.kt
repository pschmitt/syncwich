package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Body for `POST /api/households/mealplans` - Mealie v3.22.0's `CreatePlanEntry` schema, confirmed
 * against the live instance's `/openapi.json`. Only `date` is required; `entryType` defaults to
 * `"breakfast"`, `title`/`text` default to blank, and `recipeId` is an optional recipe UUID for
 * linking the entry to an existing recipe.
 */
@Serializable
data class CreatePlanEntryDto(
    val date: String,
    val entryType: String = "breakfast",
    val title: String = "",
    val text: String = "",
    val recipeId: String? = null,
)

/**
 * Body for `PUT /api/households/mealplans/{id}` - Mealie v3.22.0's `UpdatePlanEntry` schema,
 * confirmed against the live instance's `/openapi.json`. Unlike [CreatePlanEntryDto], the server
 * additionally requires `id`, `groupId`, and `userId` - all three are round-tripped from the
 * entry's own cached [dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity] (populated from the
 * `ReadPlanEntry` response, which always includes them) rather than guessed, since they are
 * per-entry/per-user identifiers, not constants this app owns.
 */
@Serializable
data class UpdatePlanEntryDto(
    val date: String,
    val entryType: String = "breakfast",
    val title: String = "",
    val text: String = "",
    val recipeId: String? = null,
    val id: Int,
    val groupId: String,
    val userId: String,
)
