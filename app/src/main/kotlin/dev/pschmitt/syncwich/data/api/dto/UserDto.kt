package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `/api/users/self` response shape (confirmed against a live v3.22.0 Mealie instance) - only the
 * fields onboarding cares about; the endpoint returns many more (household, group, permissions).
 */
@Serializable
data class UserDto(
    val id: String? = null,
    val username: String? = null,
    val fullName: String? = null,
    val email: String? = null,
)
