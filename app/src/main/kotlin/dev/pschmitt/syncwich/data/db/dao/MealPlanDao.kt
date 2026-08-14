package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Query(
        "SELECT * FROM meal_plan_entries WHERE date BETWEEN :startDate AND :endDate " +
            "ORDER BY date ASC, entryType ASC"
    )
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<MealPlanEntryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM meal_plan_entries)")
    fun observeHasEntries(): Flow<Boolean>

    @Upsert suspend fun upsertAll(entries: List<MealPlanEntryEntity>)

    @Query("DELETE FROM meal_plan_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteRange(startDate: String, endDate: String)

    /**
     * Atomically replaces only the queried date window - unlike `RecipeDao`/`CategoryDao`'s
     * full-table replace, a meal-plan refresh is always scoped to one visible week, so wiping the
     * whole table would drop other already-cached weeks' offline data for no reason.
     */
    @Transaction
    suspend fun replaceRange(
        startDate: String,
        endDate: String,
        entries: List<MealPlanEntryEntity>,
    ) {
        deleteRange(startDate, endDate)
        upsertAll(entries)
    }
}
