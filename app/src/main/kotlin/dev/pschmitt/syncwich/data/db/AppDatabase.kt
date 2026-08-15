package dev.pschmitt.syncwich.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.dao.RecipeActionDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.RecipeStepProgressDao
import dev.pschmitt.syncwich.data.db.dao.RecipeTimelineEventDao
import dev.pschmitt.syncwich.data.db.dao.ShoppingListDao
import dev.pschmitt.syncwich.data.db.dao.TagDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeStepProgressEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import dev.pschmitt.syncwich.data.db.entity.TagEntity

/**
 * The offline recipe cache - see AGENTS.md's architecture section. Every read path in the app reads
 * from here first; the network is only ever a best-effort background refresh.
 *
 * No app has shipped with version 1 yet and every table here is a fully rebuildable server-side
 * cache, not user data, so [DatabaseModule] wires this up with `fallbackToDestructiveMigration()`
 * rather than a hand-written `Migration` - a schema bump just triggers a resync on next launch.
 */
@Database(
    entities =
        [
            RecipeSummaryEntity::class,
            RecipeDetailEntity::class,
            RecipeActionEntity::class,
            RecipeTimelineEventEntity::class,
            RecipeStepProgressEntity::class,
            CategoryEntity::class,
            TagEntity::class,
            CookbookEntity::class,
            RecipeCategoryCrossRef::class,
            RecipeTagCrossRef::class,
            ShoppingListEntity::class,
            ShoppingListItemEntity::class,
            MealPlanEntryEntity::class,
            RecipeCookbookCrossRef::class,
        ],
    // v10: SW-72 adds durable local completion state for recipe steps.
    // v9: Recover installs that were created with a conflicting v8 schema identity. The cache is
    // intentionally rebuildable, so the existing destructive-migration policy recreates it and
    // lets the next sync repopulate the database instead of crashing during Room initialization.
    // v8: SW-30 adds a durable local cache of confirmed cooking-event timeline entries ("I made
    // this"), mirroring RecipeActionEntity's pending-sync pattern. Reconciled on merge with v7
    // below, which independently bumped from the same base in a parallel worktree.
    // v7: SW-24/SW-33 add meal-plan-entry groupId/userId (needed to build the mealplan PUT route's
    // required UpdatePlanEntry body) and a durable shopping-list-item checkedPending flag (the
    // same optimistic-update-with-retry shape as recipe favorites/ratings).
    // v6: SW-33 keeps cookbook mutation fields in the offline cache so an edit does not clear
    // server-owned visibility or recipe-filter state.
    // v5: SW-24/SW-30 add durable per-user favorite/rating action state.
    // v4: SW-5 (shopping lists), SW-4 (meal plan), and SW-6 (cookbooks) each independently bumped
    // this pre-1.0, in their own worktrees, to different version numbers with different entities;
    // reconciled to v4 on merge. No migration path exists yet - see DatabaseModule's
    // fallbackToDestructiveMigration().
    version = 10,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun recipeActionDao(): RecipeActionDao

    abstract fun recipeTimelineEventDao(): RecipeTimelineEventDao

    abstract fun recipeStepProgressDao(): RecipeStepProgressDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun shoppingListDao(): ShoppingListDao

    abstract fun mealPlanDao(): MealPlanDao

    abstract fun cookbookDao(): CookbookDao

    companion object {
        const val SCHEMA_VERSION = 10
    }
}
