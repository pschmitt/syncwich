package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.MealPlanApi
import dev.pschmitt.syncwich.data.api.dto.CreatePlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.UpdatePlanEntryDto
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first meal-plan access - mirrors [RecipeRepository]/[CategoryRepository]'s
 * shape. Every read is a [Flow] from Room, scoped to a date range (a week at a time from the UI);
 * [refreshMealPlan] is a best-effort background refresh for that same range and never touches
 * cached entries outside it, so previously-synced weeks stay available offline even if only the
 * currently-viewed week is being refreshed.
 *
 * [createEntry]/[updateEntry]/[deleteEntry] (SW-24/SW-33) mirror [CookbookRepository]'s
 * network-first single-item mutation shape rather than [RecipeActionRepository]'s durable
 * pending-retry pattern: a meal-plan entry is a structural create/update/delete made through an
 * explicit save action in the UI (like the cookbook editor), not a simple per-item toggle a user
 * flips repeatedly offline, so there is no local-first optimistic write to make durable - the
 * network call runs first and only a confirmed server response is cached, leaving prior cached
 * data completely untouched on failure.
 */
@Singleton
class MealPlanRepository
@Inject
constructor(private val mealPlanApi: MealPlanApi, private val mealPlanDao: MealPlanDao) {

    fun observeMealPlan(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlanEntryEntity>> =
        mealPlanDao.observeByDateRange(startDate.toString(), endDate.toString())

    fun observeHasCachedEntries(): Flow<Boolean> = mealPlanDao.observeHasEntries()

    suspend fun refreshMealPlan(startDate: LocalDate, endDate: LocalDate): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val start = startDate.toString()
                val end = endDate.toString()
                val allItems = mutableListOf<MealPlanEntryDto>()
                var page = 1
                while (true) {
                    val response =
                        mealPlanApi.getMealPlans(
                            startDate = start,
                            endDate = end,
                            page = page,
                            perPage = MealPlanApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                mealPlanDao.replaceRange(start, end, allItems.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Meal plan refresh failed; keeping cached data") }
        }

    /**
     * Creates a meal-plan entry and immediately caches the server's returned object (which carries
     * the server-assigned `id`, `groupId`, and `userId`). A failed request leaves the existing
     * meal-plan cache completely untouched, including while offline.
     */
    suspend fun createEntry(
        date: LocalDate,
        entryType: String,
        title: String,
        text: String,
        recipeId: String?,
    ): Result<MealPlanEntryEntity> = withContext(Dispatchers.IO) {
        runCatching {
                val response =
                    mealPlanApi.createMealPlanEntry(
                        CreatePlanEntryDto(
                            date = date.toString(),
                            entryType = entryType,
                            title = title,
                            text = text,
                            recipeId = recipeId,
                        )
                    )
                val entity = response.toEntity()
                mealPlanDao.upsertAll(listOf(entity))
                entity
            }
            .onFailure { Timber.w(it, "Meal plan entry create failed; keeping cached data") }
    }

    /**
     * Updates one cached entry using the v3.22.0 single-item PUT route. `groupId`/`userId` are read
     * from the entry's own cached row rather than guessed - see [UpdatePlanEntryDto]'s kdoc.
     */
    suspend fun updateEntry(
        id: Long,
        date: LocalDate,
        entryType: String,
        title: String,
        text: String,
        recipeId: String?,
    ): Result<MealPlanEntryEntity> = withContext(Dispatchers.IO) {
        runCatching {
                val existing =
                    mealPlanDao.getById(id)
                        ?: error("This meal plan entry is not cached on this device")
                val response =
                    mealPlanApi.updateMealPlanEntry(
                        id = id.toInt(),
                        request =
                            UpdatePlanEntryDto(
                                date = date.toString(),
                                entryType = entryType,
                                title = title,
                                text = text,
                                recipeId = recipeId,
                                id = id.toInt(),
                                groupId = existing.groupId,
                                userId = existing.userId,
                            ),
                    )
                val entity = response.toEntity()
                mealPlanDao.upsertAll(listOf(entity))
                entity
            }
            .onFailure { Timber.w(it, "Meal plan entry update failed; keeping cached data") }
    }

    /** Deletes one entry from Mealie and, only on success, removes its cached row. */
    suspend fun deleteEntry(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                mealPlanApi.deleteMealPlanEntry(id.toInt()).close()
                mealPlanDao.deleteById(id)
            }
            .onFailure { Timber.w(it, "Meal plan entry delete failed; keeping cached data") }
    }

    private fun MealPlanEntryDto.toEntity() =
        MealPlanEntryEntity(
            id = id.toLong(),
            date = date,
            entryType = entryType,
            title = title,
            text = text,
            recipeId = recipeId,
            recipeName = recipe?.name,
            recipeSlug = recipe?.slug,
            recipeImage = recipe?.image,
            groupId = groupId,
            userId = userId,
        )
}
