package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/**
 * Many-to-many join between [RecipeSummaryEntity] and [CategoryEntity], rebuilt on every recipe
 * list refresh (see `RecipeRepository.refreshRecipes`) so a "filter by category" list screen is a
 * plain SQL join rather than an in-memory scan of a stored id list.
 */
@Entity(tableName = "recipe_category_cross_refs", primaryKeys = ["recipeId", "categoryId"])
data class RecipeCategoryCrossRef(val recipeId: String, val categoryId: String)
