package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.MealPlanApi
import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
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
 */
@Singleton
class MealPlanRepository
@Inject
constructor(private val mealPlanApi: MealPlanApi, private val mealPlanDao: MealPlanDao) {

    fun observeMealPlan(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlanEntryEntity>> =
        mealPlanDao.observeByDateRange(startDate.toString(), endDate.toString())

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
        )
}
