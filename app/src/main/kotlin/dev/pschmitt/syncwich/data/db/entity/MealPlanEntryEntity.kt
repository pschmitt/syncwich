package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/households/mealplans` entry, keyed by the server's own integer `id`. The nested
 * `recipe` summary is flattened into a few display columns (name/slug/image) instead of joined
 * against `RecipeSummaryEntity` - a meal-plan entry can reference a recipe the recipe cache hasn't
 * (re)synced yet, and a join that silently drops such an entry would be worse than just showing
 * these directly-stored fields. `date` is the server's `YYYY-MM-DD` string, which sorts and range-
 * filters correctly as plain text. `groupId`/`userId` (SW-24/SW-33) are cached straight from
 * `ReadPlanEntry` so [dev.pschmitt.syncwich.data.repository.MealPlanRepository.updateEntry] can
 * build the server's required `UpdatePlanEntry` body without a separate lookup.
 */
@Entity(tableName = "meal_plan_entries")
data class MealPlanEntryEntity(
    @PrimaryKey val id: Long,
    val date: String,
    val entryType: String,
    val title: String,
    val text: String,
    val recipeId: String?,
    val recipeName: String?,
    val recipeSlug: String?,
    val recipeImage: String?,
    val groupId: String = "",
    val userId: String = "",
)
