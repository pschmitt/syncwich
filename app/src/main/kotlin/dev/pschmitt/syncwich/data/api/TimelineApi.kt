package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventInDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TimelineApi {

    /**
     * Confirmed live against a real v3.22.0 Mealie instance: `GET /api/recipes/timeline/events`
     * accepts Mealie's `queryFilter` DSL (e.g. `recipeId="<uuid>"`) to scope the paginated result
     * to one recipe's cooking-event history, reusing the same [PagedResponseDto] envelope as
     * `/api/recipes`.
     */
    @GET("api/recipes/timeline/events")
    suspend fun getEvents(
        @Query("queryFilter") queryFilter: String,
        @Query("orderDirection") orderDirection: String = "desc",
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<RecipeTimelineEventDto>

    /**
     * Confirmed by the public Mealie v3.22.0 schema (`RecipeTimelineEventIn`/`RecipeTimelineEventOut`)
     * - never exercised with a live write, see `RecipeTimelineRepository`'s kdoc.
     */
    @POST("api/recipes/timeline/events")
    suspend fun createEvent(@Body request: RecipeTimelineEventInDto): RecipeTimelineEventDto

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
