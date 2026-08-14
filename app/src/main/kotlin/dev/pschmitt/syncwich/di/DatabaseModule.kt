package dev.pschmitt.syncwich.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pschmitt.syncwich.data.db.AppDatabase
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.ShoppingListDao
import dev.pschmitt.syncwich.data.db.dao.TagDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "syncwich.db")
            // No migration path exists yet (pre-release, no exported schema history to migrate
            // from) - see SW-5's TODO.md notes. Revisit once a real migration is needed.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideRecipeDao(database: AppDatabase): RecipeDao = database.recipeDao()

    @Provides fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideShoppingListDao(database: AppDatabase): ShoppingListDao = database.shoppingListDao()
}
