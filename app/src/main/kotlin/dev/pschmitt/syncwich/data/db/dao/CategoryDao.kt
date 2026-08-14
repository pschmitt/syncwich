package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories") suspend fun deleteAll()

    /**
     * Atomically replaces the whole category dictionary - used by a full refresh so readers never
     * observe a transient empty table between the delete and the re-insert.
     */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        deleteAll()
        upsertAll(categories)
    }
}
