package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListSummaryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's household shopping-list endpoints, confirmed against a live v3.22.0 instance: `GET
 * /api/households/shopping/lists` (paginated, same envelope as recipes/organizers) and `GET
 * /api/households/shopping/lists/{id}` (single list with its items).
 */
interface ShoppingListsApi {

    @GET("api/households/shopping/lists")
    suspend fun getShoppingLists(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<ShoppingListSummaryDto>

    @GET("api/households/shopping/lists/{id}")
    suspend fun getShoppingListDetail(@Path("id") id: String): ShoppingListDto

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
