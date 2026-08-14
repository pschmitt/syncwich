package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mealie's household meal-plan endpoint, confirmed against a live v3.22.0 instance: `GET
 * /api/households/mealplans` returns the same `{page,per_page,total,total_pages,items,next,
 * previous}` envelope as `/api/recipes`, `/api/organizers/categories`, and `/api/organizers/tags`
 * (`PlanEntryPagination` in the server's `/openapi.json`), filtered by the inclusive
 * `start_date`/`end_date` query params (`YYYY-MM-DD`, confirmed accepted by a live request).
 * Unlike the other paginated endpoints, `perPage` here defaults to a full week's worth of entries
 * rather than 50, since callers always query one week at a time.
 */
interface MealPlanApi {

    @GET("api/households/mealplans")
    suspend fun getMealPlans(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<MealPlanEntryDto>

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
