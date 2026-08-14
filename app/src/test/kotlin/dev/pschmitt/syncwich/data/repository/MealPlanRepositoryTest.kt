package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.MealPlanApi
import dev.pschmitt.syncwich.data.api.dto.CreatePlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.UpdatePlanEntryDto
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import java.io.IOException
import java.time.LocalDate
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
 * Verifies the "hard requirement" from AGENTS.md's architecture section: a failed refresh must
 * never clear or block what's already cached - see [CategoryRepositoryTest] for the same coverage
 * on the category dictionary. Also covers that a refresh only ever touches its own queried date
 * window, leaving other already-cached weeks untouched.
 */
class MealPlanRepositoryTest {

    private val start = LocalDate.of(2026, 8, 10)
    private val end = LocalDate.of(2026, 8, 16)

    @Test
    fun `refreshMealPlan replaces entries within the queried date range`() = runTest {
        val dao = FakeMealPlanDao()
        val api =
            FakeMealPlanApi(
                entries =
                    listOf(
                        MealPlanEntryDto(
                            id = 1,
                            date = "2026-08-11",
                            entryType = "dinner",
                            recipeId = "recipe-1",
                        )
                    )
            )
        val repository = MealPlanRepository(api, dao)

        val result = repository.refreshMealPlan(start, end)

        assertTrue(result.isSuccess)
        val cached = dao.observeByDateRange(start.toString(), end.toString()).first()
        assertEquals(listOf("2026-08-11"), cached.map { it.date })
        assertEquals("recipe-1", cached.single().recipeId)
    }

    @Test
    fun `a failed refreshMealPlan leaves the cache untouched`() = runTest {
        val cached =
            listOf(
                MealPlanEntryEntity(
                    id = 99,
                    date = "2026-08-11",
                    entryType = "lunch",
                    title = "Keep me",
                    text = "",
                    recipeId = null,
                    recipeName = null,
                    recipeSlug = null,
                    recipeImage = null,
                )
            )
        val dao = FakeMealPlanDao(seed = cached)
        val api = FakeMealPlanApi(failure = IOException("network down"))
        val repository = MealPlanRepository(api, dao)

        val result = repository.refreshMealPlan(start, end)

        assertTrue(result.isFailure)
        assertEquals(cached, dao.observeByDateRange(start.toString(), end.toString()).first())
    }

    @Test
    fun `refreshMealPlan does not touch cached entries outside its date range`() = runTest {
        val otherWeekEntry =
            MealPlanEntryEntity(
                id = 5,
                date = "2026-08-20",
                entryType = "breakfast",
                title = "Other week",
                text = "",
                recipeId = null,
                recipeName = null,
                recipeSlug = null,
                recipeImage = null,
            )
        val dao = FakeMealPlanDao(seed = listOf(otherWeekEntry))
        val api = FakeMealPlanApi(entries = emptyList())
        val repository = MealPlanRepository(api, dao)

        repository.refreshMealPlan(start, end)

        assertEquals(listOf(otherWeekEntry), dao.allEntries())
    }

    @Test
    fun `createEntry caches the server's returned entry`() = runTest {
        val dao = FakeMealPlanDao()
        val api = FakeMealPlanApi(createResponse = MealPlanEntryDto(id = 7, date = "2026-08-12"))
        val repository = MealPlanRepository(api, dao)

        val result =
            repository.createEntry(
                date = LocalDate.of(2026, 8, 12),
                entryType = "dinner",
                title = "Leftovers",
                text = "",
                recipeId = null,
            )

        assertTrue(result.isSuccess)
        assertEquals(7L, dao.getById(7)?.id)
    }

    @Test
    fun `createEntry failure leaves the cache untouched`() = runTest {
        val cached =
            listOf(
                MealPlanEntryEntity(
                    id = 1,
                    date = "2026-08-11",
                    entryType = "lunch",
                    title = "Keep me",
                    text = "",
                    recipeId = null,
                    recipeName = null,
                    recipeSlug = null,
                    recipeImage = null,
                )
            )
        val dao = FakeMealPlanDao(seed = cached)
        val api = FakeMealPlanApi(failure = IOException("network down"))
        val repository = MealPlanRepository(api, dao)

        val result =
            repository.createEntry(
                date = LocalDate.of(2026, 8, 12),
                entryType = "dinner",
                title = "Leftovers",
                text = "",
                recipeId = null,
            )

        assertTrue(result.isFailure)
        assertEquals(cached, dao.allEntries())
    }

    @Test
    fun `updateEntry sends the cached entry's groupId and userId`() = runTest {
        val cached =
            MealPlanEntryEntity(
                id = 42,
                date = "2026-08-11",
                entryType = "dinner",
                title = "",
                text = "",
                recipeId = null,
                recipeName = null,
                recipeSlug = null,
                recipeImage = null,
                groupId = "group-1",
                userId = "user-1",
            )
        val dao = FakeMealPlanDao(seed = listOf(cached))
        val api =
            FakeMealPlanApi(
                updateResponse =
                    MealPlanEntryDto(
                        id = 42,
                        date = "2026-08-11",
                        entryType = "dinner",
                        title = "Updated",
                        groupId = "group-1",
                        userId = "user-1",
                    )
            )
        val repository = MealPlanRepository(api, dao)

        val result =
            repository.updateEntry(
                id = 42,
                date = LocalDate.of(2026, 8, 11),
                entryType = "dinner",
                title = "Updated",
                text = "",
                recipeId = null,
            )

        assertTrue(result.isSuccess)
        assertEquals("group-1", api.lastUpdateRequest?.groupId)
        assertEquals("user-1", api.lastUpdateRequest?.userId)
        assertEquals("Updated", dao.getById(42)?.title)
    }

    @Test
    fun `deleteEntry removes the cached row only on success`() = runTest {
        val cached =
            MealPlanEntryEntity(
                id = 5,
                date = "2026-08-11",
                entryType = "dinner",
                title = "",
                text = "",
                recipeId = null,
                recipeName = null,
                recipeSlug = null,
                recipeImage = null,
            )
        val dao = FakeMealPlanDao(seed = listOf(cached))
        val api = FakeMealPlanApi()
        val repository = MealPlanRepository(api, dao)

        val result = repository.deleteEntry(5)

        assertTrue(result.isSuccess)
        assertEquals(null, dao.getById(5))
    }

    @Test
    fun `a failed deleteEntry leaves the cached row in place`() = runTest {
        val cached =
            MealPlanEntryEntity(
                id = 5,
                date = "2026-08-11",
                entryType = "dinner",
                title = "Keep me",
                text = "",
                recipeId = null,
                recipeName = null,
                recipeSlug = null,
                recipeImage = null,
            )
        val dao = FakeMealPlanDao(seed = listOf(cached))
        val api = FakeMealPlanApi(failure = IOException("network down"))
        val repository = MealPlanRepository(api, dao)

        val result = repository.deleteEntry(5)

        assertTrue(result.isFailure)
        assertEquals(cached, dao.getById(5))
    }

    private class FakeMealPlanDao(seed: List<MealPlanEntryEntity> = emptyList()) : MealPlanDao {
        private val state = MutableStateFlow(seed)

        fun allEntries(): List<MealPlanEntryEntity> = state.value

        override fun observeByDateRange(
            startDate: String,
            endDate: String,
        ): Flow<List<MealPlanEntryEntity>> {
            val range = startDate..endDate
            return state.map { entries -> entries.filter { it.date in range } }
        }

        override fun observeHasEntries(): Flow<Boolean> = state.map { it.isNotEmpty() }

        override suspend fun getById(id: Long): MealPlanEntryEntity? =
            state.value.find { it.id == id }

        override suspend fun upsertAll(entries: List<MealPlanEntryEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            entries.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteRange(startDate: String, endDate: String) {
            val range = startDate..endDate
            state.value = state.value.filterNot { it.date in range }
        }

        override suspend fun deleteById(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }
    }

    private class FakeMealPlanApi(
        private val entries: List<MealPlanEntryDto> = emptyList(),
        private val createResponse: MealPlanEntryDto? = null,
        private val updateResponse: MealPlanEntryDto? = null,
        private val failure: Throwable? = null,
    ) : MealPlanApi {
        var lastUpdateRequest: UpdatePlanEntryDto? = null

        override suspend fun getMealPlans(
            startDate: String,
            endDate: String,
            page: Int,
            perPage: Int,
        ): PagedResponseDto<MealPlanEntryDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, entries.size, entries.size, 1, entries)
        }

        override suspend fun createMealPlanEntry(request: CreatePlanEntryDto): MealPlanEntryDto {
            failure?.let { throw it }
            return createResponse ?: error("no create response configured")
        }

        override suspend fun updateMealPlanEntry(
            id: Int,
            request: UpdatePlanEntryDto,
        ): MealPlanEntryDto {
            failure?.let { throw it }
            lastUpdateRequest = request
            return updateResponse ?: error("no update response configured")
        }

        override suspend fun deleteMealPlanEntry(id: Int): ResponseBody {
            failure?.let { throw it }
            return "".toResponseBody()
        }
    }
}
