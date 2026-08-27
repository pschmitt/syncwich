package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/**
 * Many-to-many join between [RecipeSummaryEntity] and a food id (Mealie's structured ingredient
 * catalog, see `FoodEntity`) - see [RecipeCategoryCrossRef]'s kdoc, same rationale. Backs the food
 * filter added in SW-142. Unlike category/tag/tool cross-refs (built from data already embedded in
 * `/api/recipes`'s list response), this one requires each recipe's full detail to be fetched at
 * least once - see [RecipeDetailEntity.sourceUpdatedAt] and
 * [dev.pschmitt.syncwich.data.repository.RecipeRepository]'s bulk detail fetch.
 */
@Entity(tableName = "recipe_food_cross_refs", primaryKeys = ["recipeId", "foodId"])
data class RecipeFoodCrossRef(val recipeId: String, val foodId: String)
