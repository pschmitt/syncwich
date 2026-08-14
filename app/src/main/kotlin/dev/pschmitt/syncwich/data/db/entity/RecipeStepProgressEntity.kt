package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/** Local, per-recipe completion state for the read-only step checklist. */
@Entity(tableName = "recipe_step_progress", primaryKeys = ["recipeId", "stepIndex"])
data class RecipeStepProgressEntity(
    val recipeId: String,
    val stepIndex: Int,
    val completed: Boolean = true,
    val updatedAt: Long = 0L,
)
