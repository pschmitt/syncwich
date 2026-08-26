package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.FoodsApi
import dev.pschmitt.syncwich.data.api.dto.FoodDto
import dev.pschmitt.syncwich.data.api.dto.FoodMutationDto
import dev.pschmitt.syncwich.data.db.dao.FoodDao
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first access to Mealie's structured ingredient-food catalog - mirrors
 * [TagRepository]'s refresh shape and [CookbookRepository]'s mutation shape.
 */
@Singleton
class FoodRepository
@Inject
constructor(private val foodsApi: FoodsApi, private val foodDao: FoodDao) {

    fun observeFoods(): Flow<List<FoodEntity>> = foodDao.observeAll()

    fun observeFood(foodId: String): Flow<FoodEntity?> = foodDao.observeById(foodId)

    suspend fun refreshFoods(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val allItems = mutableListOf<FoodDto>()
                var page = 1
                while (true) {
                    val response =
                        foodsApi.getFoods(page = page, perPage = FoodsApi.DEFAULT_PAGE_SIZE)
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                foodDao.replaceAll(allItems.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Food refresh failed; keeping cached data") }
        }

    /** Creates a food and immediately caches the returned server object. */
    suspend fun createFood(request: FoodMutationDto): Result<FoodEntity> = mutateFood {
        foodsApi.createFood(request)
    }

    /** Updates one food using the single-item PUT route and refreshes its Room row. */
    suspend fun updateFood(foodId: String, request: FoodMutationDto): Result<FoodEntity> =
        mutateFood {
            foodsApi.updateFood(foodId, request)
        }

    /** Deletes the server food first; its cached row is then removed. */
    suspend fun deleteFood(foodId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                foodsApi.deleteFood(foodId).use {}
                foodDao.deleteById(foodId)
            }
                .onFailure {
                    Timber.w(it, "Food deletion failed for '$foodId'; keeping cached data")
                }
        }

    private suspend fun mutateFood(request: suspend () -> FoodDto): Result<FoodEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = request().toEntity()
                foodDao.upsertAll(listOf(entity))
                entity
            }
                .onFailure { Timber.w(it, "Food mutation failed; keeping cached data") }
        }

    private fun FoodDto.toEntity() =
        FoodEntity(id = id, name = name, pluralName = pluralName, description = description)
}
