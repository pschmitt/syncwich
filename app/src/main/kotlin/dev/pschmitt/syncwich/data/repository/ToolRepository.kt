package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.api.dto.OrganizerMutationDto
import dev.pschmitt.syncwich.data.db.dao.ToolDao
import dev.pschmitt.syncwich.data.db.entity.ToolEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Cache-first, offline-first tool access - mirrors [CategoryRepository]'s shape exactly. */
@Singleton
class ToolRepository
@Inject
constructor(private val organizersApi: OrganizersApi, private val toolDao: ToolDao) {

    fun observeTools(): Flow<List<ToolEntity>> = toolDao.observeAll()

    fun observeTool(toolId: String): Flow<ToolEntity?> = toolDao.observeById(toolId)

    suspend fun refreshTools(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val allItems = mutableListOf<OrganizerDto>()
                var page = 1
                while (true) {
                    val response =
                        organizersApi.getTools(
                            page = page,
                            perPage = OrganizersApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                toolDao.replaceAll(allItems.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Tool refresh failed; keeping cached data") }
        }

    suspend fun createTool(name: String): Result<ToolEntity> =
        mutate { organizersApi.createTool(OrganizerMutationDto(name)) }

    suspend fun updateTool(toolId: String, name: String): Result<ToolEntity> =
        mutate { organizersApi.updateTool(toolId, OrganizerMutationDto(name)) }

    suspend fun deleteTool(toolId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    organizersApi.deleteTool(toolId).use {}
                    toolDao.deleteById(toolId)
                }
                .onFailure { Timber.w(it, "Tool deletion failed for '$toolId'; keeping cached data") }
        }

    private suspend fun mutate(request: suspend () -> OrganizerDto): Result<ToolEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val entity = request().toEntity()
                    toolDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Tool mutation failed; keeping cached data") }
        }

    private fun OrganizerDto.toEntity() = ToolEntity(id = id, name = name, slug = slug)
}
