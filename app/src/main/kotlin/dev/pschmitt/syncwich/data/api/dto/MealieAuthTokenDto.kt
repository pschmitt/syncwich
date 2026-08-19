package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The short-lived Mealie JWT returned by password and OIDC login endpoints. */
@Serializable
data class MealieAuthTokenDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "bearer",
)
