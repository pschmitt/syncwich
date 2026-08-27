package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Mealie's household recipe-action item (`/api/households/recipe-actions`), confirmed live via
 * `/openapi.json` (`GroupRecipeActionOut`) - a per-recipe triggerable automation (e.g. a webhook),
 * a distinct Mealie concept from this app's own `RecipeActionEntity` (the local favorite/rating
 * cache), hence the different local name (SW-139). `actionType` is Mealie's `"link"`/`"post"` enum.
 */
@Serializable
data class RecipeAutomationDto(
    val id: String,
    val actionType: String,
    val title: String,
    val url: String,
    val groupId: String,
    val householdId: String,
)

/** `POST /api/households/recipe-actions` body (`CreateGroupRecipeAction`). */
@Serializable
data class RecipeAutomationCreateDto(val actionType: String, val title: String, val url: String)

/**
 * `PUT /api/households/recipe-actions/{id}` body (`SaveGroupRecipeAction`) - requires [groupId] and
 * [householdId] round-tripped from the cached [RecipeAutomationDto] (see
 * `RecipeAutomationRepository.updateAutomation`).
 */
@Serializable
data class RecipeAutomationSaveDto(
    val actionType: String,
    val title: String,
    val url: String,
    val groupId: String,
    val householdId: String,
)
