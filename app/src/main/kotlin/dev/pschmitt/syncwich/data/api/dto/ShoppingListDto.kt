package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `/api/households/shopping/lists` list item shape, confirmed against a live v3.22.0 Mealie
 * instance - same `PagedResponseDto` envelope as `/api/recipes` and the `/api/organizers`
 * endpoints. Unlike the detail endpoint, list items here carry no `listItems` array (item counts
 * aren't exposed at this level), only enough to render a list-of-lists screen.
 */
@Serializable
data class ShoppingListSummaryDto(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * `/api/households/shopping/lists/{id}` response shape, confirmed live - same list metadata as
 * [ShoppingListSummaryDto] plus the actual `listItems` array.
 */
@Serializable
data class ShoppingListDto(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val listItems: List<ShoppingListItemDto> = emptyList(),
)

/**
 * One shopping-list item, confirmed live. `display` is Mealie's own pre-formatted human-readable
 * rendering (quantity + unit + food + note collapsed into one string, e.g. "2 cups flour") - the
 * same approach used for recipe ingredients - so it's the only text field this read-only client
 * needs to show; `checked` is the view-only state this app displays but never mutates.
 */
@Serializable
data class ShoppingListItemDto(
    val id: String,
    val shoppingListId: String,
    val display: String,
    val note: String? = null,
    val checked: Boolean = false,
    val position: Int = 0,
)
