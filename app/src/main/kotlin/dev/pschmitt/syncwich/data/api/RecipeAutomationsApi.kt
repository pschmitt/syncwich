package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationCreateDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationDto
import dev.pschmitt.syncwich.data.api.dto.RecipeAutomationSaveDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's household recipe-action catalog (`/api/households/recipe-actions`), confirmed live via
 * `/openapi.json`: `GET/POST /api/households/recipe-actions` (same `PagedResponseDto` envelope as
 * `/api/recipes`) and `GET/PUT/DELETE /api/households/recipe-actions/{item_id}` (SW-139).
 */
interface RecipeAutomationsApi {

    @GET("api/households/recipe-actions")
    suspend fun getRecipeAutomations(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<RecipeAutomationDto>

    @POST("api/households/recipe-actions")
    suspend fun createRecipeAutomation(
        @Body request: RecipeAutomationCreateDto
    ): RecipeAutomationDto

    @PUT("api/households/recipe-actions/{itemId}")
    suspend fun updateRecipeAutomation(
        @Path("itemId") itemId: String,
        @Body request: RecipeAutomationSaveDto,
    ): RecipeAutomationDto

    @DELETE("api/households/recipe-actions/{itemId}")
    suspend fun deleteRecipeAutomation(@Path("itemId") itemId: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
