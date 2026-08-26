package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.FoodsApi
import dev.pschmitt.syncwich.data.api.dto.FoodDto
import dev.pschmitt.syncwich.data.api.dto.FoodMutationDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.db.dao.FoodDao
import dev.pschmitt.syncwich.data.db.entity.FoodEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the "hard requirement" from AGENTS.md's architecture section: a failed refresh/mutation
 * must never clear or block what's already cached - see [CookbookRepositoryTest]'s equivalent
 * coverage.
 */
class FoodRepositoryTest {

    @Test
    fun `successful food create caches the complete returned entity`() = runTest {
        val foodDao = FakeFoodDao()
        val created = FoodDto(id = "created-1", name = "Flour", pluralName = null, description = "")
        val repository = FoodRepository(FakeFoodsApi(createResponse = created), foodDao)

        val result = repository.createFood(FoodMutationDto(name = "Flour"))

        assertTrue(result.isSuccess)
        assertEquals(
            FoodEntity(id = "created-1", name = "Flour", pluralName = null, description = ""),
            foodDao.observeAll().first().single(),
        )
    }

    @Test
    fun `failed food create leaves the existing cache untouched`() = runTest {
        val cached = FoodEntity("keep-1", "Keep me", null, "")
        val foodDao = FakeFoodDao(seed = listOf(cached))
        val repository =
            FoodRepository(FakeFoodsApi(mutationFailure = IOException("offline")), foodDao)

        val result = repository.createFood(FoodMutationDto(name = "New food"))

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), foodDao.observeAll().first())
    }

    @Test
    fun `successful food delete removes its cached row`() = runTest {
        val foodDao = FakeFoodDao(seed = listOf(FoodEntity("remove-1", "Remove me", null, "")))
        val repository = FoodRepository(FakeFoodsApi(), foodDao)

        assertTrue(repository.deleteFood("remove-1").isSuccess)
        assertEquals(emptyList<FoodEntity>(), foodDao.observeAll().first())
    }

    @Test
    fun `failed food delete leaves the cached row untouched`() = runTest {
        val cached = FoodEntity("keep-1", "Keep me", null, "")
        val foodDao = FakeFoodDao(seed = listOf(cached))
        val repository =
            FoodRepository(FakeFoodsApi(mutationFailure = IOException("offline")), foodDao)

        val result = repository.deleteFood("keep-1")

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), foodDao.observeAll().first())
    }

    @Test
    fun `refreshFoods replaces the cache on success`() = runTest {
        val foodDao = FakeFoodDao(seed = listOf(FoodEntity("old-1", "Old", null, "")))
        val foodsApi = FakeFoodsApi(foods = listOf(FoodDto("new-1", "New", null, "")))
        val repository = FoodRepository(foodsApi, foodDao)

        val result = repository.refreshFoods()

        assertTrue(result.isSuccess)
        assertEquals(listOf("New"), foodDao.observeAll().first().map { it.name })
    }

    @Test
    fun `a failed refreshFoods leaves the cache untouched`() = runTest {
        val cached = listOf(FoodEntity("keep-1", "Keep Me", null, ""))
        val foodDao = FakeFoodDao(seed = cached)
        val repository =
            FoodRepository(FakeFoodsApi(failure = IOException("network down")), foodDao)

        val result = repository.refreshFoods()

        assertTrue(result.isFailure)
        assertEquals(cached, foodDao.observeAll().first())
    }

    private class FakeFoodDao(seed: List<FoodEntity> = emptyList()) : FoodDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<FoodEntity>> = state

        override fun observeById(id: String): Flow<FoodEntity?> = state.map { list ->
            list.find { it.id == id }
        }

        override suspend fun upsertAll(foods: List<FoodEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            foods.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeFoodsApi(
        private val foods: List<FoodDto> = emptyList(),
        private val failure: Throwable? = null,
        private val createResponse: FoodDto? = null,
        private val updateResponse: FoodDto? = null,
        private val mutationFailure: Throwable? = null,
    ) : FoodsApi {

        override suspend fun getFoods(page: Int, perPage: Int): PagedResponseDto<FoodDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, foods.size, foods.size, 1, foods)
        }

        override suspend fun createFood(request: FoodMutationDto): FoodDto =
            mutationFailure?.let { throw it }
                ?: createResponse
                ?: error("not used by FoodRepositoryTest")

        override suspend fun updateFood(itemId: String, request: FoodMutationDto): FoodDto =
            mutationFailure?.let { throw it }
                ?: updateResponse
                ?: error("not used by FoodRepositoryTest")

        override suspend fun deleteFood(itemId: String): ResponseBody {
            mutationFailure?.let { throw it }
            return "".toResponseBody()
        }
    }
}
