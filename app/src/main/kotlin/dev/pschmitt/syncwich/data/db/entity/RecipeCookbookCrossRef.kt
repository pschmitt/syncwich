package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity

/**
 * Many-to-many join between [RecipeSummaryEntity] and [CookbookEntity], rebuilt on every cookbook
 * refresh (see `CookbookRepository.refreshCookbooks`) since a cookbook's membership is a live
 * server-side filter, not a stored id list - see [CookbookEntity]'s kdoc.
 */
@Entity(tableName = "recipe_cookbook_cross_refs", primaryKeys = ["recipeId", "cookbookId"])
data class RecipeCookbookCrossRef(val recipeId: String, val cookbookId: String)
