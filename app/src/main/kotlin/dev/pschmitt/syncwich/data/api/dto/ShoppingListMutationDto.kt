package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Body for `POST /api/households/shopping/items` - Mealie v3.22.0's `ShoppingListItemCreate`
 * schema, confirmed against the live instance's `/openapi.json`. Only `shoppingListId` is
 * required; a freeform item (no linked food/unit/recipe) is created by setting `display` alone,
 * the same way Mealie's own apps let a user type a plain-text shopping-list line.
 */
@Serializable
data class CreateShoppingListItemDto(
    val shoppingListId: String,
    val display: String = "",
    val note: String? = null,
    val checked: Boolean = false,
    val position: Int = 0,
)

/**
 * Response envelope for both `POST /api/households/shopping/items` and the single/bulk-item PUT
 * routes - Mealie's `ShoppingListItemsCollectionOut` schema, confirmed against the live instance's
 * `/openapi.json`. Only `createdItems` is decoded here (the only field
 * [dev.pschmitt.syncwich.data.repository.ShoppingListRepository.addItem] needs); `updatedItems`/
 * `deletedItems` are dropped by the app's global `ignoreUnknownKeys = true` `Json` config.
 */
@Serializable
data class ShoppingListItemsCollectionDto(val createdItems: List<ShoppingListItemDto> = emptyList())
