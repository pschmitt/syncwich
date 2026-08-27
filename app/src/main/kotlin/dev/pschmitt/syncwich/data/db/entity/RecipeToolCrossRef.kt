package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/**
 * Many-to-many join between [RecipeSummaryEntity] and [ToolEntity] - see [RecipeCategoryCrossRef]'s
 * kdoc, same rationale. Backs the tools filter added in SW-142.
 */
@Entity(tableName = "recipe_tool_cross_refs", primaryKeys = ["recipeId", "toolId"])
data class RecipeToolCrossRef(val recipeId: String, val toolId: String)
