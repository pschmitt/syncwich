package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.RecipeAutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeAutomationDao {

    @Query("SELECT * FROM recipe_automations ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RecipeAutomationEntity>>

    @Query("SELECT * FROM recipe_automations WHERE id = :id")
    fun observeById(id: String): Flow<RecipeAutomationEntity?>

    @Upsert suspend fun upsertAll(automations: List<RecipeAutomationEntity>)

    @Query("DELETE FROM recipe_automations WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM recipe_automations") suspend fun deleteAll()

    /**
     * Atomically replaces the whole recipe-action dictionary - see [CategoryDao.replaceAll]'s kdoc.
     */
    @Transaction
    suspend fun replaceAll(automations: List<RecipeAutomationEntity>) {
        deleteAll()
        upsertAll(automations)
    }
}
