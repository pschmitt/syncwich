package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One `/api/households/shopping/lists` item. */
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAt: String?,
)
