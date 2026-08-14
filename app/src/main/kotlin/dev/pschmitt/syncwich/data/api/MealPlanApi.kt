package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.CreatePlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.UpdatePlanEntryDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's household meal-plan endpoint, confirmed against a live v3.22.0 instance: `GET
 * /api/households/mealplans` returns the same `{page,per_page,total,total_pages,items,next,
 * previous}` envelope as `/api/recipes`, `/api/organizers/categories`, and `/api/organizers/tags`
 * (`PlanEntryPagination` in the server's `/openapi.json`), filtered by the inclusive
 * `start_date`/`end_date` query params (`YYYY-MM-DD`, confirmed accepted by a live request). Unlike
 * the other paginated endpoints, `perPage` here defaults to a full week's worth of entries rather
 * than 50, since callers always query one week at a time.
 *
 * The single-item mutation routes (SW-24/SW-33) were confirmed by reading the same live instance's
 * `/openapi.json` `paths`/`components.schemas` sections - no write request was made: `POST
 * /api/households/mealplans` ("Create One", `CreatePlanEntry` -> `ReadPlanEntry`), `PUT
 * /api/households/mealplans/{item_id}` ("Update One", `UpdatePlanEntry` -> `ReadPlanEntry`), and
 * `DELETE /api/households/mealplans/{item_id}` ("Delete One", no body, returns `ReadPlanEntry`).
 */
interface MealPlanApi {

    @GET("api/households/mealplans")
    suspend fun getMealPlans(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<MealPlanEntryDto>

    @POST("api/households/mealplans")
    suspend fun createMealPlanEntry(@Body request: CreatePlanEntryDto): MealPlanEntryDto

    @PUT("api/households/mealplans/{id}")
    suspend fun updateMealPlanEntry(
        @Path("id") id: Int,
        @Body request: UpdatePlanEntryDto,
    ): MealPlanEntryDto

    @DELETE("api/households/mealplans/{id}")
    suspend fun deleteMealPlanEntry(@Path("id") id: Int): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
