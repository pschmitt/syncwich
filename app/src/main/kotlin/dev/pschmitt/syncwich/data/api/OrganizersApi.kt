package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mealie's category/tag endpoints, confirmed against a live v3.22.0 instance: `GET
 * /api/organizers/categories` and `GET /api/organizers/tags`, both returning the same
 * `PagedResponseDto<OrganizerDto>` envelope as `/api/recipes`.
 */
interface OrganizersApi {

    @GET("api/organizers/categories")
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<OrganizerDto>

    @GET("api/organizers/tags")
    suspend fun getTags(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<OrganizerDto>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
