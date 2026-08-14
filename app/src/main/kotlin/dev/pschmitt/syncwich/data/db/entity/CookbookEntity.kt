package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/households/cookbooks` item - a saved recipe-category/tag filter, not an embedded recipe
 * list. Which recipes currently match it is cached separately in [RecipeCookbookCrossRef].
 */
@Entity(tableName = "cookbooks")
data class CookbookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val position: Int,
    val public: Boolean = false,
    val queryFilterString: String = "",
)
