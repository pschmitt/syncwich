package dev.pschmitt.syncwich.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.ShoppingListDao
import dev.pschmitt.syncwich.data.db.dao.TagDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
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
            ShoppingListEntity::class,
            ShoppingListItemEntity::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun shoppingListDao(): ShoppingListDao
}
