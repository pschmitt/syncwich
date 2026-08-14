package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Shared shape for both `/api/organizers/categories` and `/api/organizers/tags` items, and for the
 * `recipeCategory`/`tags` arrays embedded in recipe list/detail responses - all confirmed identical
 * against a live v3.22.0 Mealie instance, e.g.
 * `{"id":"...","groupId":"...","name":"Backen","slug":"backen"}`.
 */
@Serializable
data class OrganizerDto(
    val id: String,
    val groupId: String? = null,
    val name: String,
    val slug: String,
)
