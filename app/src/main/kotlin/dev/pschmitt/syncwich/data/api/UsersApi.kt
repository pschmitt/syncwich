package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.UserDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingSummariesDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingUpdateDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UsersApi {
    /** Used by onboarding to validate the entered server URL + API token before saving them. */
    @GET("api/users/self") suspend fun getSelf(): UserDto

    /** Read-only bootstrap for the offline recipe action cache. */
    @GET("api/users/self/favorites")
    suspend fun getSelfFavorites(): UserRatingSummariesDto

    /** Read-only bootstrap for the offline recipe action cache. */
    @GET("api/users/self/ratings")
    suspend fun getSelfRatings(): UserRatingSummariesDto

    /** Confirmed by the public Mealie v3.22.0 schema; callers update Room before invoking it. */
    @POST("api/users/{id}/favorites/{slug}")
    suspend fun addFavorite(
        @Path("id") userId: String,
        @Path("slug") recipeSlug: String,
    ): ResponseBody

    /** Confirmed by the public Mealie v3.22.0 schema; callers update Room before invoking it. */
    @DELETE("api/users/{id}/favorites/{slug}")
    suspend fun removeFavorite(
        @Path("id") userId: String,
        @Path("slug") recipeSlug: String,
    ): ResponseBody

    /** Confirmed by the public Mealie v3.22.0 schema; callers update Room before invoking it. */
    @POST("api/users/{id}/ratings/{slug}")
    suspend fun updateRating(
        @Path("id") userId: String,
        @Path("slug") recipeSlug: String,
        @Body request: UserRatingUpdateDto,
    ): ResponseBody
}
