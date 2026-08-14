package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeActionDao {
    @Query("SELECT * FROM recipe_actions WHERE recipeId = :recipeId")
    fun observe(recipeId: String): Flow<RecipeActionEntity?>

    @Query("SELECT * FROM recipe_actions WHERE recipeId = :recipeId")
    suspend fun get(recipeId: String): RecipeActionEntity?

    @Query("SELECT * FROM recipe_actions ORDER BY recipeId ASC")
    suspend fun getAll(): List<RecipeActionEntity>

    @Query(
        "SELECT * FROM recipe_actions WHERE favoritePending = 1 OR ratingPending = 1 " +
            "ORDER BY recipeId ASC"
    )
    suspend fun getPending(): List<RecipeActionEntity>

    @Upsert suspend fun upsert(action: RecipeActionEntity)

    @Upsert suspend fun upsertAll(actions: List<RecipeActionEntity>)
}
