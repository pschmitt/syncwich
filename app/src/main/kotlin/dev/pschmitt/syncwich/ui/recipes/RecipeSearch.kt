package dev.pschmitt.syncwich.ui.recipes

import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity

/**
 * In-memory name/description search over an already-loaded (category- or tag-filtered) recipe
 * list - the cached recipe count is small enough that a Room `LIKE` query would be overkill, and
 * this keeps search composable with [dev.pschmitt.syncwich.data.repository.RecipeRepository]'s
 * existing category/tag `Flow` queries instead of needing a new one.
 */
fun filterRecipesByQuery(
    recipes: List<RecipeSummaryEntity>,
    query: String,
): List<RecipeSummaryEntity> {
    if (query.isBlank()) return recipes
    return recipes.filter {
        it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
    }
}
