package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.OrganizerMutationDto
import dev.pschmitt.syncwich.data.db.dao.CategoryDao
import dev.pschmitt.syncwich.data.db.entity.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first category access - mirrors [RecipeRepository]'s shape ([TagRepository]
 * does the same for tags). Every read is a [Flow] from Room; [refreshCategories] is a best-effort
 * background refresh, authoritative for the full category dictionary (including categories with no
 * recipes, which never show up via [RecipeRepository.refreshRecipes]'s embedded upserts).
 */
@Singleton
class CategoryRepository
@Inject
constructor(private val organizersApi: OrganizersApi, private val categoryDao: CategoryDao) {

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeCategory(categoryId: String): Flow<CategoryEntity?> =
        categoryDao.observeById(categoryId)

    suspend fun refreshCategories(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val allItems = mutableListOf<OrganizerDto>()
                var page = 1
                while (true) {
                    val response =
                        organizersApi.getCategories(
                            page = page,
                            perPage = OrganizersApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                categoryDao.replaceAll(allItems.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Category refresh failed; keeping cached data") }
        }

    suspend fun createCategory(name: String): Result<CategoryEntity> =
        mutate { organizersApi.createCategory(OrganizerMutationDto(name)) }

    suspend fun updateCategory(categoryId: String, name: String): Result<CategoryEntity> =
        mutate { organizersApi.updateCategory(categoryId, OrganizerMutationDto(name)) }

    suspend fun deleteCategory(categoryId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    organizersApi.deleteCategory(categoryId).use {}
                    categoryDao.deleteById(categoryId)
                }
                .onFailure {
                    Timber.w(it, "Category deletion failed for '$categoryId'; keeping cached data")
                }
        }

    private suspend fun mutate(request: suspend () -> OrganizerDto): Result<CategoryEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val entity = request().toEntity()
                    categoryDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Category mutation failed; keeping cached data") }
        }

    private fun OrganizerDto.toEntity() = CategoryEntity(id = id, name = name, slug = slug)
}
