package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeStepProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeStepProgressDao {
    @Query(
        "SELECT * FROM recipe_step_progress WHERE recipeId = :recipeId AND completed = 1 " +
            "ORDER BY stepIndex ASC"
    )
    fun observeCompleted(recipeId: String): Flow<List<RecipeStepProgressEntity>>

    @Upsert suspend fun upsert(progress: RecipeStepProgressEntity)

    @Query("DELETE FROM recipe_step_progress WHERE recipeId = :recipeId AND stepIndex = :stepIndex")
    suspend fun delete(recipeId: String, stepIndex: Int)

    @Query("DELETE FROM recipe_step_progress WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)
}
