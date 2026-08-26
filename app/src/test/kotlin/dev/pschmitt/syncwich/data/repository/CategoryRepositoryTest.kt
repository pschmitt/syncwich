package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.OrganizerMutationDto
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
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
 * must never clear or block what's already cached. [TagRepository]/[ToolRepository] mirror
 * [CategoryRepository]'s shape exactly, so this coverage stands in for all three (SW-139).
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

    @Test
    fun `successful category create caches the complete returned entity`() = runTest {
        val dao = FakeCategoryDao()
        val created = OrganizerDto("created-1", null, "Desserts", "desserts")
        val repository = CategoryRepository(FakeOrganizersApi(createResponse = created), dao)

        val result = repository.createCategory("Desserts")

        assertTrue(result.isSuccess)
        assertEquals(
            CategoryEntity("created-1", "Desserts", "desserts"),
            dao.observeAll().first().single(),
        )
    }

    @Test
    fun `failed category delete leaves the cached row untouched`() = runTest {
        val cached = CategoryEntity("keep-1", "Keep me", "keep-me")
        val dao = FakeCategoryDao(seed = listOf(cached))
        val repository =
            CategoryRepository(FakeOrganizersApi(mutationFailure = IOException("offline")), dao)

        val result = repository.deleteCategory("keep-1")

        assertTrue(result.isFailure)
        assertEquals(listOf(cached), dao.observeAll().first())
    }

    private class FakeCategoryDao(seed: List<CategoryEntity> = emptyList()) : CategoryDao {
        private val state = MutableStateFlow(seed)

        override fun observeAll(): Flow<List<CategoryEntity>> = state

        override fun observeById(id: String): Flow<CategoryEntity?> = state.map { list ->
            list.find { it.id == id }
        }

        override suspend fun upsertAll(categories: List<CategoryEntity>) {
            val byId = state.value.associateBy { it.id }.toMutableMap()
            categories.forEach { byId[it.id] = it }
            state.value = byId.values.toList()
        }

        override suspend fun deleteById(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    private class FakeOrganizersApi(
        private val categories: List<OrganizerDto> = emptyList(),
        private val failure: Throwable? = null,
        private val createResponse: OrganizerDto? = null,
        private val mutationFailure: Throwable? = null,
    ) : OrganizersApi {
        override suspend fun getCategories(
            page: Int,
            perPage: Int,
        ): PagedResponseDto<OrganizerDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, categories.size, categories.size, 1, categories)
        }

        override suspend fun createCategory(request: OrganizerMutationDto): OrganizerDto =
            mutationFailure?.let { throw it }
                ?: createResponse
                ?: error("not used by CategoryRepositoryTest")

        override suspend fun updateCategory(
            itemId: String,
            request: OrganizerMutationDto,
        ): OrganizerDto = error("not used by CategoryRepositoryTest")

        override suspend fun deleteCategory(itemId: String): ResponseBody {
            mutationFailure?.let { throw it }
            return "".toResponseBody()
        }

        override suspend fun getTags(page: Int, perPage: Int): PagedResponseDto<OrganizerDto> =
            error("not used by CategoryRepository")

        override suspend fun createTag(request: OrganizerMutationDto): OrganizerDto =
            error("not used by CategoryRepository")

        override suspend fun updateTag(
            itemId: String,
            request: OrganizerMutationDto,
        ): OrganizerDto = error("not used by CategoryRepository")

        override suspend fun deleteTag(itemId: String): ResponseBody =
            error("not used by CategoryRepository")

        override suspend fun getTools(page: Int, perPage: Int): PagedResponseDto<OrganizerDto> =
            error("not used by CategoryRepository")

        override suspend fun createTool(request: OrganizerMutationDto): OrganizerDto =
            error("not used by CategoryRepository")

        override suspend fun updateTool(
            itemId: String,
            request: OrganizerMutationDto,
        ): OrganizerDto = error("not used by CategoryRepository")

        override suspend fun deleteTool(itemId: String): ResponseBody =
            error("not used by CategoryRepository")
    }
}
