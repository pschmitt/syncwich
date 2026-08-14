package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/api/auth/token` response: Mealie's short-lived (~48h) login JWT, used exactly once to mint a
 * long-lived API token (see [dev.pschmitt.syncwich.data.onboarding.PasswordTokenMinter]) and never
 * persisted.
 */
@Serializable
data class PasswordLoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String? = null,
)

/** `/api/users/api-tokens` request body. */
@Serializable
data class LongLiveTokenRequestDto(val name: String, val integrationId: String)

/**
 * `/api/users/api-tokens` response (confirmed live against v3.22.0). [token] is the long-lived
 * API token itself - the only field this app persists (via `SettingsRepository`).
 */
@Serializable
data class LongLiveTokenResponseDto(
    val name: String,
    val id: Int,
    val createdAt: String? = null,
    val token: String,
)
