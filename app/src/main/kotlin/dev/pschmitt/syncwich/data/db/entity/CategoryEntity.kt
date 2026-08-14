package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One `/api/organizers/categories` item. */
@Entity(tableName = "categories")
data class CategoryEntity(@PrimaryKey val id: String, val name: String, val slug: String)
