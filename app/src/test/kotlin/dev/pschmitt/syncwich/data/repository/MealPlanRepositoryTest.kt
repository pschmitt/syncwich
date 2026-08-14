package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.MealPlanApi
import dev.pschmitt.syncwich.data.api.dto.MealPlanEntryDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.db.dao.MealPlanDao
import dev.pschmitt.syncwich.data.db.entity.MealPlanEntryEntity
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
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

        override suspend fun upsertAll(entries: List<MealPlanEntryEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            entries.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteRange(startDate: String, endDate: String) {
            val range = startDate..endDate
            state.value = state.value.filterNot { it.date in range }
        }
    }

    private class FakeMealPlanApi(
        private val entries: List<MealPlanEntryDto> = emptyList(),
        private val failure: Throwable? = null,
    ) : MealPlanApi {
        override suspend fun getMealPlans(
            startDate: String,
            endDate: String,
            page: Int,
            perPage: Int,
        ): PagedResponseDto<MealPlanEntryDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, entries.size, entries.size, 1, entries)
        }
    }
}
