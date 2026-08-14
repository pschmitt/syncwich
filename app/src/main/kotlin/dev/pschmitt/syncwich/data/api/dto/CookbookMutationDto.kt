package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Body used by Mealie v3.22.0 for both cookbook creation and single-item update. The public OpenAPI
 * schema calls this `CreateCookBook`, including when it is used by the PUT route.
 */
@Serializable
data class CreateCookbookDto(
    val name: String,
    val description: String = "",
    val position: Int = 1,
    val public: Boolean = false,
    val queryFilterString: String = "",
    val slug: String? = null,
)
