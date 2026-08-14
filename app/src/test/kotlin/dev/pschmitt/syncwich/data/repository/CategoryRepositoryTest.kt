package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the "hard requirement" from AGENTS.md's architecture section: a failed refresh must
 * never clear or block what's already cached. [TagRepository] mirrors [CategoryRepository]'s shape
 * exactly, so this coverage stands in for both.
 */
class CategoryRepositoryTest {

    @Test
    fun `refreshCategories replaces the cache on success`() = runTest {
        val dao = FakeCategoryDao(seed = listOf(CategoryEntity("old-1", "Old", "old")))
        val api = FakeOrganizersApi(categories = listOf(OrganizerDto("new-1", null, "New", "new")))
        val repository = CategoryRepository(api, dao)

        val result = repository.refreshCategories()

        assertTrue(result.isSuccess)
        assertEquals(listOf("New"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun `a failed refreshCategories leaves the cache untouched`() = runTest {
        val cached = listOf(CategoryEntity("keep-1", "Keep Me", "keep-me"))
        val dao = FakeCategoryDao(seed = cached)
        val api = FakeOrganizersApi(failure = IOException("network down"))
        val repository = CategoryRepository(api, dao)

        val result = repository.refreshCategories()

        assertTrue(result.isFailure)
        assertEquals(cached, dao.observeAll().first())
    }

    private class FakeCategoryDao(seed: List<CategoryEntity> = emptyList()) : CategoryDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<CategoryEntity>> = state

        override suspend fun upsertAll(categories: List<CategoryEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            categories.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeOrganizersApi(
        private val categories: List<OrganizerDto> = emptyList(),
        private val failure: Throwable? = null,
    ) : OrganizersApi {
        override suspend fun getCategories(
            page: Int,
            perPage: Int,
        ): PagedResponseDto<OrganizerDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, categories.size, categories.size, 1, categories)
        }

        override suspend fun getTags(page: Int, perPage: Int): PagedResponseDto<OrganizerDto> =
            error("not used by CategoryRepository")
    }
}
