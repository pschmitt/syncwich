package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {

    @Query("SELECT * FROM tools ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE id = :id") fun observeById(id: String): Flow<ToolEntity?>

    @Upsert suspend fun upsertAll(tools: List<ToolEntity>)

    @Query("DELETE FROM tools WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM tools") suspend fun deleteAll()

    /** Atomically replaces the whole tool dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(tools: List<ToolEntity>) {
        deleteAll()
        upsertAll(tools)
    }
}
