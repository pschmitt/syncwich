package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeCookbookCrossRef
import dev.pschmitt.syncwich.data.db.entity.RecipeDetailEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeSummaryEntity
import dev.pschmitt.syncwich.data.db.entity.RecipeTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipe_summaries ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RecipeSummaryEntity>>

    @Query(
        """
        SELECT recipe_summaries.* FROM recipe_summaries
        INNER JOIN recipe_category_cross_refs
            ON recipe_summaries.id = recipe_category_cross_refs.recipeId
        WHERE recipe_category_cross_refs.categoryId = :categoryId
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByCategory(categoryId: String): Flow<List<RecipeSummaryEntity>>

    @Query(
        """
        SELECT recipe_summaries.* FROM recipe_summaries
        INNER JOIN recipe_tag_cross_refs ON recipe_summaries.id = recipe_tag_cross_refs.recipeId
        WHERE recipe_tag_cross_refs.tagId = :tagId
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByTag(tagId: String): Flow<List<RecipeSummaryEntity>>

    @Query("SELECT * FROM recipe_details WHERE id = :id")
    fun observeDetail(id: String): Flow<RecipeDetailEntity?>

    @Query("SELECT * FROM recipe_details WHERE slug = :slug LIMIT 1")
    fun observeDetailBySlug(slug: String): Flow<RecipeDetailEntity?>

    @Query("SELECT * FROM recipe_summaries ORDER BY id ASC")
    suspend fun getAll(): List<RecipeSummaryEntity>

    @Query("SELECT * FROM recipe_details ORDER BY id ASC")
    suspend fun getAllDetails(): List<RecipeDetailEntity>

    @Query(
        """
        SELECT recipe_summaries.* FROM recipe_summaries
        INNER JOIN recipe_cookbook_cross_refs
            ON recipe_summaries.id = recipe_cookbook_cross_refs.recipeId
        WHERE recipe_cookbook_cross_refs.cookbookId = :cookbookId
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByCookbook(cookbookId: String): Flow<List<RecipeSummaryEntity>>

    @Upsert suspend fun upsertAll(recipes: List<RecipeSummaryEntity>)

    @Upsert suspend fun upsertDetail(detail: RecipeDetailEntity)

    @Insert suspend fun insertCategoryCrossRefs(refs: List<RecipeCategoryCrossRef>)

    @Insert suspend fun insertTagCrossRefs(refs: List<RecipeTagCrossRef>)

    @Insert suspend fun insertCookbookCrossRefs(refs: List<RecipeCookbookCrossRef>)

    @Query("DELETE FROM recipe_summaries") suspend fun deleteAll()

    @Query("DELETE FROM recipe_category_cross_refs") suspend fun deleteAllCategoryCrossRefs()

    @Query("DELETE FROM recipe_tag_cross_refs") suspend fun deleteAllTagCrossRefs()

    @Query("DELETE FROM recipe_cookbook_cross_refs") suspend fun deleteAllCookbookCrossRefs()

    @Query("DELETE FROM recipe_cookbook_cross_refs WHERE cookbookId = :cookbookId")
    suspend fun deleteCookbookCrossRefs(cookbookId: String)

    @Query("DELETE FROM recipe_summaries WHERE id = :recipeId")
    suspend fun deleteSummary(recipeId: String)

    @Query("DELETE FROM recipe_details WHERE id = :recipeId")
    suspend fun deleteDetail(recipeId: String)

    @Query("DELETE FROM recipe_category_cross_refs WHERE recipeId = :recipeId")
    suspend fun deleteCategoryCrossRefs(recipeId: String)

    @Query("DELETE FROM recipe_tag_cross_refs WHERE recipeId = :recipeId")
    suspend fun deleteTagCrossRefs(recipeId: String)

    @Query("DELETE FROM recipe_cookbook_cross_refs WHERE recipeId = :recipeId")
    suspend fun deleteRecipeCookbookCrossRefs(recipeId: String)

    /** Removes every cached representation of one recipe after a confirmed server deletion. */
    @Transaction
    suspend fun deleteRecipeCache(recipeId: String) {
        deleteSummary(recipeId)
        deleteDetail(recipeId)
        deleteCategoryCrossRefs(recipeId)
        deleteTagCrossRefs(recipeId)
        deleteRecipeCookbookCrossRefs(recipeId)
    }

    /** Atomically replaces the complete cached cookbook membership set. */
    @Transaction
    suspend fun replaceCookbookCrossRefs(refs: List<RecipeCookbookCrossRef>) {
        deleteAllCookbookCrossRefs()
        if (refs.isNotEmpty()) insertCookbookCrossRefs(refs)
    }

    /**
     * Atomically refreshes one cookbook's summaries and membership without touching other books.
     */
    @Transaction
    suspend fun replaceCookbookRecipeCache(
        cookbookId: String,
        recipes: List<RecipeSummaryEntity>,
        refs: List<RecipeCookbookCrossRef>,
    ) {
        if (recipes.isNotEmpty()) upsertAll(recipes)
        deleteCookbookCrossRefs(cookbookId)
        if (refs.isNotEmpty()) insertCookbookCrossRefs(refs)
    }
}
