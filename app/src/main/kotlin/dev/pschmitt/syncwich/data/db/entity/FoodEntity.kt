package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One cached `/api/foods` item - see [dev.pschmitt.syncwich.data.api.dto.FoodDto]'s kdoc. */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pluralName: String?,
    val description: String,
)
