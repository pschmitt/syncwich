package dev.pschmitt.syncwich.data.api

import dev.pschmitt.syncwich.data.api.dto.CreateShoppingListItemDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListItemsCollectionDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListSummaryDto
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mealie's household shopping-list endpoints, confirmed against a live v3.22.0 instance: `GET
 * /api/households/shopping/lists` (paginated, same envelope as recipes/organizers) and `GET
 * /api/households/shopping/lists/{id}` (single list with its items).
 *
 * The item mutation routes (SW-24/SW-33) were confirmed by reading the same live instance's
 * `/openapi.json` `paths`/`components.schemas` sections - no write request was made: `POST
 * /api/households/shopping/items` ("Create One", `ShoppingListItemCreate` ->
 * `ShoppingListItemsCollectionOut`), `PUT /api/households/shopping/items/{item_id}` ("Update One",
 * `ShoppingListItemUpdate`), and `DELETE /api/households/shopping/items/{item_id}` ("Delete One",
 * no body).
 *
 * [getShoppingItemRaw]/[updateShoppingItemRaw] deliberately decode the single-item GET/PUT as an
 * opaque [JsonObject] rather than a typed DTO: `ShoppingListItemUpdate` is a full-replace body, and
 * this app's cached [dev.pschmitt.syncwich.data.api.dto.ShoppingListItemDto] only tracks a handful
 * of display fields (not quantity/unit/food/label/extras/recipe references). Whether Mealie's PUT
 * handler resets omitted fields to their schema defaults or preserves the existing row can't be
 * determined from the public schema alone, and this app must never risk silently discarding a
 * user's real shopping-list item data over that ambiguity. So a checked-state toggle always fetches
 * the item's full current JSON representation first, flips only the `checked` key, and PUTs that
 * same object back - correct regardless of which update semantics the server actually implements.
 */
interface ShoppingListsApi {

    @GET("api/households/shopping/lists")
    suspend fun getShoppingLists(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = DEFAULT_PAGE_SIZE,
    ): PagedResponseDto<ShoppingListSummaryDto>

    @GET("api/households/shopping/lists/{id}")
    suspend fun getShoppingListDetail(@Path("id") id: String): ShoppingListDto

    @GET("api/households/shopping/items/{id}")
    suspend fun getShoppingItemRaw(@Path("id") id: String): JsonObject

    @PUT("api/households/shopping/items/{id}")
    suspend fun updateShoppingItemRaw(@Path("id") id: String, @Body body: JsonObject): JsonObject

    @POST("api/households/shopping/items")
    suspend fun createShoppingItem(
        @Body request: CreateShoppingListItemDto
    ): ShoppingListItemsCollectionDto

    @DELETE("api/households/shopping/items/{id}")
    suspend fun deleteShoppingItem(@Path("id") id: String): ResponseBody

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
