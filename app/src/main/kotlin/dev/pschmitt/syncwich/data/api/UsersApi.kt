package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.UserDto
import retrofit2.http.GET

interface UsersApi {
    /** Used by onboarding to validate the entered server URL + API token before saving them. */
    @GET("api/users/self") suspend fun getSelf(): UserDto
}
