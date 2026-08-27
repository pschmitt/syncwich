package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.UnitDto
import dev.pschmitt.syncwich.data.api.dto.UnitMutationDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's ingredient-unit catalog (`/api/units`), confirmed live via `/openapi.json`: `GET/POST
 * /api/units` (same `PagedResponseDto` envelope as `/api/recipes`) and `GET/PUT/DELETE
 * /api/units/{item_id}` (SW-139). The PUT body is the same `CreateIngredientUnit` shape as POST.
 */
interface UnitsApi {

    @GET("api/units")
    suspend fun getUnits(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<UnitDto>

    @POST("api/units") suspend fun createUnit(@Body request: UnitMutationDto): UnitDto

    @PUT("api/units/{itemId}")
    suspend fun updateUnit(
        @Path("itemId") itemId: String,
        @Body request: UnitMutationDto,
    ): UnitDto

    @DELETE("api/units/{itemId}")
    suspend fun deleteUnit(@Path("itemId") itemId: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
