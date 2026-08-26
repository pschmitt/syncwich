package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.OrganizerMutationDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's category/tag/tool endpoints, confirmed against a live v3.22.0/v3.24.0 instance: `GET/
 * POST /api/organizers/categories`, `/api/organizers/tags`, and `/api/organizers/tools`, all
 * returning the same `PagedResponseDto<OrganizerDto>` envelope as `/api/recipes`; single-item
 * `GET/PUT/DELETE /api/organizers/<resource>/{item_id}` (SW-139).
 */
interface OrganizersApi {

    @GET("api/organizers/categories")
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<OrganizerDto>

    @POST("api/organizers/categories")
    suspend fun createCategory(@Body request: OrganizerMutationDto): OrganizerDto

    @PUT("api/organizers/categories/{itemId}")
    suspend fun updateCategory(
        @Path("itemId") itemId: String,
        @Body request: OrganizerMutationDto,
    ): OrganizerDto

    @DELETE("api/organizers/categories/{itemId}")
    suspend fun deleteCategory(@Path("itemId") itemId: String): ResponseBody

    @GET("api/organizers/tags")
    suspend fun getTags(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<OrganizerDto>

    @POST("api/organizers/tags")
    suspend fun createTag(@Body request: OrganizerMutationDto): OrganizerDto

    @PUT("api/organizers/tags/{itemId}")
    suspend fun updateTag(
        @Path("itemId") itemId: String,
        @Body request: OrganizerMutationDto,
    ): OrganizerDto

    @DELETE("api/organizers/tags/{itemId}")
    suspend fun deleteTag(@Path("itemId") itemId: String): ResponseBody

    @GET("api/organizers/tools")
    suspend fun getTools(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<OrganizerDto>

    @POST("api/organizers/tools")
    suspend fun createTool(@Body request: OrganizerMutationDto): OrganizerDto

    @PUT("api/organizers/tools/{itemId}")
    suspend fun updateTool(
        @Path("itemId") itemId: String,
        @Body request: OrganizerMutationDto,
    ): OrganizerDto

    @DELETE("api/organizers/tools/{itemId}")
    suspend fun deleteTool(@Path("itemId") itemId: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
