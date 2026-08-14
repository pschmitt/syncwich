package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One item of a `/api/households/shopping/lists/{id}` response - `display` is Mealie's own
 * pre-formatted rendering, see [dev.pschmitt.syncwich.data.api.dto.ShoppingListItemDto]. `checked`
 * is toggleable (SW-24/SW-33); `checkedPending` mirrors `RecipeActionEntity.favoritePending` - it
 * marks an offline/failed checked-state sync durable until
 * [dev.pschmitt.syncwich.data.repository.ShoppingListRepository.syncPendingItemChecks] can retry
 * it, the same optimistic-update-with-retry shape used for recipe favorites/ratings.
 */
@Entity(
    tableName = "shopping_list_items",
    foreignKeys =
        [
            ForeignKey(
                entity = ShoppingListEntity::class,
                parentColumns = ["id"],
                childColumns = ["shoppingListId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("shoppingListId")],
)
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val shoppingListId: String,
    val display: String,
    val note: String?,
    val checked: Boolean,
    val position: Int,
    val checkedPending: Boolean = false,
)
