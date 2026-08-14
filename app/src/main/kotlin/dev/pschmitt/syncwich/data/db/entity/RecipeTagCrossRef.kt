package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/**
 * Many-to-many join between [RecipeSummaryEntity] and [TagEntity] - see [RecipeCategoryCrossRef]'s
 * kdoc, same rationale.
 */
@Entity(tableName = "recipe_tag_cross_refs", primaryKeys = ["recipeId", "tagId"])
data class RecipeTagCrossRef(val recipeId: String, val tagId: String)
