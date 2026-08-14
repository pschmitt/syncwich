package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.CookbookDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mealie's cookbook endpoint, confirmed against a live v3.22.0 instance: `GET
 * /api/households/cookbooks` returns the same paginated envelope as `/api/recipes` and
 * `/api/organizers/categories`. A cookbook's member recipes aren't embedded in this response - see
 * [RecipesApi.getRecipesByCookbook].
 */
interface CookbooksApi {

    @GET("api/households/cookbooks")
    suspend fun getCookbooks(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<CookbookDto>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
