package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeTimelineEventDao {
    @Query(
        "SELECT * FROM recipe_timeline_events WHERE recipeId = :recipeId ORDER BY timestamp DESC"
    )
    fun observeForRecipe(recipeId: String): Flow<List<RecipeTimelineEventEntity>>

    @Query(
        "SELECT * FROM recipe_timeline_events WHERE recipeId = :recipeId ORDER BY timestamp DESC"
    )
    suspend fun getForRecipe(recipeId: String): List<RecipeTimelineEventEntity>

    @Query("SELECT * FROM recipe_timeline_events WHERE pending = 1 ORDER BY updatedAt ASC")
    suspend fun getPending(): List<RecipeTimelineEventEntity>

    @Upsert suspend fun upsert(event: RecipeTimelineEventEntity)

    @Upsert suspend fun upsertAll(events: List<RecipeTimelineEventEntity>)

    @Query("DELETE FROM recipe_timeline_events WHERE localId = :localId")
    suspend fun delete(localId: String)
}
