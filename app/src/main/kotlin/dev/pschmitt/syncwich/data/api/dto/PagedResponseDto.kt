package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mealie's standard pagination envelope, confirmed against a live v3.22.0 instance for
 * `/api/recipes`, `/api/organizers/categories`, and `/api/organizers/tags` - e.g.
 * `{"page":1,"per_page":3,"total":63,"total_pages":21,"items":[...],"next":"...","previous":null}`.
 */
@Serializable
data class PagedResponseDto<T>(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    @SerialName("total_pages") val totalPages: Int,
    val items: List<T>,
    val next: String? = null,
    val previous: String? = null,
)
