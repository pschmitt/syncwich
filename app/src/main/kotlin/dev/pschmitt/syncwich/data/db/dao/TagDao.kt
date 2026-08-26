package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id") fun observeById(id: String): Flow<TagEntity?>

    @Upsert suspend fun upsertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM tags") suspend fun deleteAll()

    /** Atomically replaces the whole tag dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(tags: List<TagEntity>) {
        deleteAll()
        upsertAll(tags)
    }
}
