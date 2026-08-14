package dev.pschmitt.syncwich.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.TagDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import dev.pschmitt.syncwich.data.db.entity.TagEntity

/**
 * The offline recipe cache - see AGENTS.md's architecture section. Every read path in the app reads
 * from here first; the network is only ever a best-effort background refresh.
 */
@Database(
    entities =
        [
            RecipeSummaryEntity::class,
            RecipeDetailEntity::class,
            CategoryEntity::class,
            TagEntity::class,
            RecipeCategoryCrossRef::class,
            RecipeTagCrossRef::class,
            MealPlanEntryEntity::class,
        ],
    // v2 (SW-4): added meal_plan_entries. No migration path exists yet pre-1.0 - see
    // DatabaseModule's fallbackToDestructiveMigration().
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun mealPlanDao(): MealPlanDao
}
