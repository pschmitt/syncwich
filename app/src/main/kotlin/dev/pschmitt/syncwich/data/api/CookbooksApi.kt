package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.CookbookDto
import dev.pschmitt.syncwich.data.api.dto.CreateCookbookDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's cookbook endpoint, confirmed against a live v3.22.0 instance: `GET
 * /api/households/cookbooks` returns the same paginated envelope as `/api/recipes` and
 * `/api/organizers/categories`. A cookbook's member recipes aren't embedded in this response - see
 * [RecipesApi.getRecipesByCookbook].
 */
interface CookbooksApi {

    /** `POST /api/households/cookbooks` accepts the public schema's `CreateCookBook` body. */
    @POST("api/households/cookbooks")
    suspend fun createCookbook(@Body request: CreateCookbookDto): CookbookDto

    /**
     * Mealie uses the same `CreateCookBook` body for the single-item PUT route. This is distinct
     * from its bulk PUT route, which accepts an array of `UpdateCookBook` objects.
     */
    @PUT("api/households/cookbooks/{itemId}")
    suspend fun updateCookbook(
        @Path("itemId") itemId: String,
        @Body request: CreateCookbookDto,
    ): CookbookDto

    @GET("api/households/cookbooks")
    suspend fun getCookbooks(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<CookbookDto>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
