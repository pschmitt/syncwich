package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-user recipe actions. Pending flags make an optimistic offline change durable until a later
 * foreground/background sync can retry it; this table is deliberately separate from the server's
 * recipe cache because favorite/rating state belongs to the authenticated user.
 */
@Entity(tableName = "recipe_actions")
data class RecipeActionEntity(
    @PrimaryKey val recipeId: String,
    val recipeSlug: String,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val favoritePending: Boolean = false,
    val ratingPending: Boolean = false,
    val updatedAt: Long = 0L,
)
