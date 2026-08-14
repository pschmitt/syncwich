package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.CreateRecipeDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeInputDto
import dev.pschmitt.syncwich.data.api.dto.RecipeSummaryDto
import dev.pschmitt.syncwich.data.api.dto.ScrapeRecipeDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipesApi {

    /**
     * Mealie v3.22.0's `POST /api/recipes` accepts `CreateRecipe` and returns a plain string. The
     * response stays raw because the public schema does not promise whether that string is an id or
     * slug.
     */
    @POST("api/recipes") suspend fun createRecipe(@Body request: CreateRecipeDto): ResponseBody

    /** Confirmed against Mealie's live OpenAPI: parses a public recipe URL into a saved recipe. */
    @POST("api/recipes/create/url")
    suspend fun parseRecipeUrl(@Body request: ScrapeRecipeDto): ResponseBody

    /** `PUT /api/recipes/{slug}` accepts the complete `Recipe-Input` object. */
    @PUT("api/recipes/{slug}")
    suspend fun updateRecipe(
        @Path("slug") slug: String,
        @Body request: RecipeInputDto,
    ): ResponseBody

    /** `PATCH /api/recipes/{slug}` has the same request shape as the single-item PUT route. */
    @PATCH("api/recipes/{slug}")
    suspend fun patchRecipe(
        @Path("slug") slug: String,
        @Body request: RecipeInputDto,
    ): ResponseBody

    /** Confirmed by the live Mealie OpenAPI schema: deletes one recipe by slug. */
    @DELETE("api/recipes/{slug}")
    suspend fun deleteRecipe(@Path("slug") slug: String): ResponseBody

    /** Confirmed by the live Mealie OpenAPI schema: multipart cover-image replacement. */
    @Multipart
    @PUT("api/recipes/{slug}/image")
    suspend fun updateRecipeImage(
        @Path("slug") slug: String,
        @Part image: okhttp3.MultipartBody.Part,
        @Part("extension") extension: okhttp3.RequestBody,
    ): ResponseBody

    @DELETE("api/recipes/{slug}/image")
    suspend fun deleteRecipeImage(@Path("slug") slug: String): ResponseBody

    /** Confirmed by the live Mealie OpenAPI schema: multipart recipe asset upload. */
    @Multipart
    @POST("api/recipes/{slug}/assets")
    suspend fun uploadRecipeAsset(
        @Path("slug") slug: String,
        @Part("name") name: okhttp3.RequestBody,
        @Part("icon") icon: okhttp3.RequestBody,
        @Part("extension") extension: okhttp3.RequestBody,
        @Part file: okhttp3.MultipartBody.Part,
    ): dev.pschmitt.syncwich.data.api.dto.RecipeAssetDto

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

    /**
     * Recipes currently matching one cookbook's saved filter - confirmed live as `GET
     * /api/recipes?cookbook={id}`, same paginated envelope as the plain recipe list. See
     * `CookbookDto`'s kdoc for why a cookbook's recipes aren't embedded in its own response.
     */
    @GET("api/recipes")
    suspend fun getRecipesByCookbook(
        @Query("cookbook") cookbookId: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<RecipeSummaryDto>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
