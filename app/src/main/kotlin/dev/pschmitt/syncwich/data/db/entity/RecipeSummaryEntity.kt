package dev.pschmitt.syncwich.data.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/recipes` list item. Real columns for everything the recipe list/search/sort/filter UI
 * needs; category/tag membership lives in [RecipeCategoryCrossRef]/[RecipeTagCrossRef] rather than
 * a stored id list, so filtering by category or tag is a plain SQL join instead of an in-memory
 * scan. The full recipe (ingredients, instructions, nutrition) is deliberately *not* here - see
 * [RecipeDetailEntity].
 */
@Entity(tableName = "recipe_summaries")
@Immutable
data class RecipeSummaryEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val name: String,
    val description: String,
    /**
     * Mealie's `image` field - a cache-busting version string, not a URL. Absent (null) means the
     * recipe has no image. The actual image is fetched from
     * `{serverUrl}/api/media/recipes/{id}/images/min-original.webp` (confirmed live) and cached by
     * Coil, per AGENTS.md - never duplicated into Room.
     */
    val image: String?,
    val rating: Double?,
    val prepTime: String?,
    val totalTime: String?,
    val dateAdded: String?,
    val lastMade: String?,
)
