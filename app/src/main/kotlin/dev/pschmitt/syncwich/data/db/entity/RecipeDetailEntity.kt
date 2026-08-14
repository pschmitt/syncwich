package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The full `/api/recipes/{slug}` response, stored verbatim as a JSON text column - deliberately not
 * deep-normalized into Room columns/join tables (ingredients, instructions, nutrition, assets stay
 * nested JSON), decoded at read time by the consumer via
 * `Json.decodeFromString<dev.pschmitt.syncwich.data.api.dto.RecipeDetailDto>(detailJson)`. See
 * AGENTS.md's offline-cache section for why.
 */
@Entity(tableName = "recipe_details")
data class RecipeDetailEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val detailJson: String,
    val fetchedAt: Long,
)
