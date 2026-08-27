package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.LabelCreateDto
import dev.pschmitt.syncwich.data.api.dto.LabelDto
import dev.pschmitt.syncwich.data.api.dto.LabelUpdateDto
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
 * Mealie's multi-purpose label catalog (`/api/groups/labels`), confirmed live via `/openapi.json`:
 * `GET/POST /api/groups/labels` (same `PagedResponseDto` envelope as `/api/recipes`) and
 * `GET/PUT/DELETE /api/groups/labels/{item_id}` (SW-139).
 */
interface LabelsApi {

    @GET("api/groups/labels")
    suspend fun getLabels(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<LabelDto>

    @POST("api/groups/labels") suspend fun createLabel(@Body request: LabelCreateDto): LabelDto

    @PUT("api/groups/labels/{itemId}")
    suspend fun updateLabel(
        @Path("itemId") itemId: String,
        @Body request: LabelUpdateDto,
    ): LabelDto

    @DELETE("api/groups/labels/{itemId}")
    suspend fun deleteLabel(@Path("itemId") itemId: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
