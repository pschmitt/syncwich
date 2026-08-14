package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeCategoryCrossRef
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

    @Upsert suspend fun upsertAll(recipes: List<RecipeSummaryEntity>)

    @Upsert suspend fun upsertDetail(detail: RecipeDetailEntity)

    @Insert suspend fun insertCategoryCrossRefs(refs: List<RecipeCategoryCrossRef>)

    @Insert suspend fun insertTagCrossRefs(refs: List<RecipeTagCrossRef>)

    @Query("DELETE FROM recipe_summaries") suspend fun deleteAll()

    @Query("DELETE FROM recipe_category_cross_refs") suspend fun deleteAllCategoryCrossRefs()

    @Query("DELETE FROM recipe_tag_cross_refs") suspend fun deleteAllTagCrossRefs()
}
