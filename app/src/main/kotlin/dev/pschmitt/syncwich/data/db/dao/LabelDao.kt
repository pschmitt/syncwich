package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :id") fun observeById(id: String): Flow<LabelEntity?>

    @Upsert suspend fun upsertAll(labels: List<LabelEntity>)

    @Query("DELETE FROM labels WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM labels") suspend fun deleteAll()

    /** Atomically replaces the whole label dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(labels: List<LabelEntity>) {
        deleteAll()
        upsertAll(labels)
    }
}
