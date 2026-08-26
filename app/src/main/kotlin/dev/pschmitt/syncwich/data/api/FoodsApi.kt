package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.FoodDto
import dev.pschmitt.syncwich.data.api.dto.FoodMutationDto
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
 * Mealie's structured ingredient-food catalog, confirmed against a live v3.24.0 instance's
 * `/openapi.json`: `GET/POST /api/foods` (same `PagedResponseDto` envelope as `/api/recipes`) and
 * `GET/PUT/DELETE /api/foods/{item_id}`. The PUT body is the same `CreateIngredientFood` shape as
 * POST (id comes from the path, not the body).
 */
interface FoodsApi {

    @GET("api/foods")
    suspend fun getFoods(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<FoodDto>

    @POST("api/foods") suspend fun createFood(@Body request: FoodMutationDto): FoodDto

    @PUT("api/foods/{itemId}")
    suspend fun updateFood(
        @Path("itemId") itemId: String,
        @Body request: FoodMutationDto,
    ): FoodDto

    @DELETE("api/foods/{itemId}")
    suspend fun deleteFood(@Path("itemId") itemId: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
