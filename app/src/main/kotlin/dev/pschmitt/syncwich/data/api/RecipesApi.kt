package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipesApi {

    @GET("api/recipes")
    suspend fun getRecipes(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<RecipeSummaryDto>

    /**
     * Returns the raw response body rather than a decoded DTO - the repository stores this
     * endpoint's JSON verbatim in Room (see `RecipeDetailEntity`) instead of remapping it through a
     * partial model, so no field it doesn't itself need is ever silently dropped.
     */
    @GET("api/recipes/{slug}")
    suspend fun getRecipeDetailRaw(@Path("slug") slug: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
