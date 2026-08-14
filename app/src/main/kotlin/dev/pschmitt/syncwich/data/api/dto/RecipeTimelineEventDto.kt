package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `RecipeTimelineEventOut` - confirmed against a live v3.22.0 Mealie instance's
 * `GET /api/recipes/timeline/events?queryFilter=recipeId="<uuid>"`. Real events observed on that
 * instance include Mealie's own `system`-type "Recipe Created" entries as well as
 * `comment`-type "<user's full name> made this" entries recorded by Mealie's official clients -
 * the shape [dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository.recordMadeThis]
 * mirrors. Only the fields this read-only client needs are modeled; the live response also
 * includes `groupId`/`householdId`/`updatedAt`, which `ignoreUnknownKeys` safely drops.
 */
@Serializable
data class RecipeTimelineEventDto(
    val id: String,
    val recipeId: String,
    val userId: String? = null,
    val subject: String,
    val eventType: String,
    val eventMessage: String? = null,
    val timestamp: String,
)

/**
 * Request body for `POST /api/recipes/timeline/events` (`RecipeTimelineEventIn` in the public
 * v3.22.0 schema). The endpoint itself was never exercised with a live write - see
 * `RecipeTimelineRepository`'s kdoc - so this shape is confirmed from the schema plus the real
 * `comment`-type "<user> made this" events read back from a live instance, not from a live POST.
 */
@Serializable
data class RecipeTimelineEventInDto(
    val recipeId: String,
    val subject: String,
    val eventType: String = "comment",
    val eventMessage: String? = null,
    val timestamp: String,
)
