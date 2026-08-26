package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id") fun observeById(id: String): Flow<FoodEntity?>

    @Upsert suspend fun upsertAll(foods: List<FoodEntity>)

    @Query("DELETE FROM foods WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM foods") suspend fun deleteAll()

    /** Atomically replaces the whole food dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(foods: List<FoodEntity>) {
        deleteAll()
        upsertAll(foods)
    }
}
