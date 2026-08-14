package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class UserRatingSummaryDto(
    val recipeId: String,
    val rating: Double? = null,
    val isFavorite: Boolean = false,
)

@Serializable data class UserRatingSummariesDto(val ratings: List<UserRatingSummaryDto>)

/**
 * Request body for `POST /api/users/{id}/ratings/{slug}`. [rating] is a JSON element so a null
 * rating can explicitly clear an existing rating even though the app's shared Json instance uses
 * `explicitNulls = false` for response decoding.
 */
@Serializable
data class UserRatingUpdateDto(
    val rating: JsonElement? = null,
    val isFavorite: Boolean? = null,
) {
    companion object {
        fun forRating(rating: Int?): UserRatingUpdateDto =
            UserRatingUpdateDto(rating = rating?.let(::JsonPrimitive) ?: JsonNull)
    }
}
