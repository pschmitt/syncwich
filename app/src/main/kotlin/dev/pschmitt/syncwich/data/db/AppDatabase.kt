package dev.pschmitt.syncwich.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.TagDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import dev.pschmitt.syncwich.data.db.entity.TagEntity

/**
 * The offline recipe cache - see AGENTS.md's architecture section. Every read path in the app reads
 * from here first; the network is only ever a best-effort background refresh.
 *
 * Version 2 (SW-6) added the cookbook tables. No app has shipped with version 1 yet and every table
 * here is a fully rebuildable server-side cache, not user data, so [DatabaseModule] wires this up
 * with `fallbackToDestructiveMigration()` rather than a hand-written `Migration` - a schema bump
 * just triggers a resync on next launch.
 */
@Database(
    entities =
        [
            RecipeSummaryEntity::class,
            RecipeDetailEntity::class,
            CategoryEntity::class,
            TagEntity::class,
            CookbookEntity::class,
            RecipeCategoryCrossRef::class,
            RecipeTagCrossRef::class,
            RecipeCookbookCrossRef::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun cookbookDao(): CookbookDao
}
