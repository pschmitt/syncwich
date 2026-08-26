package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Shared create/update request body for `/api/organizers/categories`, `/api/organizers/tags`, and
 * `/api/organizers/tools` - confirmed live (read-only `/openapi.json` inspection) that all three
 * accept just `{"name": "..."}` (`CategoryIn`/`TagIn`/`RecipeToolCreate`'s only required field).
 */
@Serializable data class OrganizerMutationDto(val name: String)
