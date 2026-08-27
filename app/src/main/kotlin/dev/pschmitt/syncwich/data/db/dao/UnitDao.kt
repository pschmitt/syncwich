package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {

    @Query("SELECT * FROM units ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :id") fun observeById(id: String): Flow<UnitEntity?>

    @Upsert suspend fun upsertAll(units: List<UnitEntity>)

    @Query("DELETE FROM units WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM units") suspend fun deleteAll()

    /** Atomically replaces the whole unit dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(units: List<UnitEntity>) {
        deleteAll()
        upsertAll(units)
    }
}
