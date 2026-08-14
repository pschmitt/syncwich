package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One item of a `/api/households/shopping/lists/{id}` response - `display` is Mealie's own
 * pre-formatted rendering, see [dev.pschmitt.syncwich.data.api.dto.ShoppingListItemDto]. `checked`
 * is shown but never mutated - this app is read-only.
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
)
