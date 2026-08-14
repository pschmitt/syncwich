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
import dev.pschmitt.syncwich.data.db.dao.CookbookDao
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.dao.RecipeDao
import dev.pschmitt.syncwich.data.db.dao.RecipeActionDao
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
            // Every table here is a fully rebuildable server-side cache, not user data - see
            // AppDatabase's version changelog comment - so a schema bump just wipes and resyncs
            // rather than needing a hand-written Migration.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideRecipeDao(database: AppDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideRecipeActionDao(database: AppDatabase): RecipeActionDao = database.recipeActionDao()

    @Provides fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideShoppingListDao(database: AppDatabase): ShoppingListDao = database.shoppingListDao()

    @Provides fun provideMealPlanDao(database: AppDatabase): MealPlanDao = database.mealPlanDao()

    @Provides fun provideCookbookDao(database: AppDatabase): CookbookDao = database.cookbookDao()
}
