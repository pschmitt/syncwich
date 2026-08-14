package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `/api/recipes` list item shape, confirmed against a live v3.22.0 Mealie instance. Only the fields
 * the recipe list/filter UI needs are modeled - `ignoreUnknownKeys` drops the rest (userId,
 * householdId, groupId, recipeServings, orgURL, createdAt, updatedAt, etc.).
 */
@Serializable
data class RecipeSummaryDto(
    val id: String,
    val slug: String,
    val name: String,
    val image: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val prepTime: String? = null,
    val totalTime: String? = null,
    val dateAdded: String? = null,
    val lastMade: String? = null,
    val recipeCategory: List<OrganizerDto> = emptyList(),
    val tags: List<OrganizerDto> = emptyList(),
)
