package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.LabelsApi
import dev.pschmitt.syncwich.data.api.dto.LabelCreateDto
import dev.pschmitt.syncwich.data.api.dto.LabelDto
import dev.pschmitt.syncwich.data.api.dto.LabelUpdateDto
import dev.pschmitt.syncwich.data.db.dao.LabelDao
import dev.pschmitt.syncwich.data.db.entity.LabelEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first label access - mirrors [CategoryRepository]'s shape, except
 * [updateLabel] must round-trip [LabelEntity.groupId] into the request body (see
 * [LabelUpdateDto]'s kdoc), unlike Categories/Tags/Tools where the id alone is enough.
 */
@Singleton
class LabelRepository
@Inject
constructor(private val labelsApi: LabelsApi, private val labelDao: LabelDao) {

    fun observeLabels(): Flow<List<LabelEntity>> = labelDao.observeAll()

    fun observeLabel(labelId: String): Flow<LabelEntity?> = labelDao.observeById(labelId)

    suspend fun refreshLabels(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val allItems = mutableListOf<LabelDto>()
                    var page = 1
                    while (true) {
                        val response =
                            labelsApi.getLabels(page = page, perPage = LabelsApi.DEFAULT_PAGE_SIZE)
                        allItems += response.items
                        if (response.items.isEmpty() || page >= response.totalPages) break
                        page++
                    }
                    labelDao.replaceAll(allItems.map { it.toEntity() })
                }
                .onFailure { Timber.w(it, "Label refresh failed; keeping cached data") }
        }

    suspend fun createLabel(name: String, color: String): Result<LabelEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val entity = labelsApi.createLabel(LabelCreateDto(name, color)).toEntity()
                    labelDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Label creation failed; keeping cached data") }
        }

    suspend fun updateLabel(labelId: String, name: String, color: String): Result<LabelEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val cached =
                        labelDao.observeById(labelId).first()
                            ?: error("No cached label '$labelId' to round-trip groupId from")
                    val entity =
                        labelsApi
                            .updateLabel(
                                labelId,
                                LabelUpdateDto(
                                    name = name,
                                    color = color,
                                    groupId = cached.groupId,
                                    id = labelId,
                                ),
                            )
                            .toEntity()
                    labelDao.upsertAll(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Label update failed for '$labelId'; keeping cached data") }
        }

    suspend fun deleteLabel(labelId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                    labelsApi.deleteLabel(labelId).use {}
                    labelDao.deleteById(labelId)
                }
                .onFailure {
                    Timber.w(it, "Label deletion failed for '$labelId'; keeping cached data")
                }
        }

    private fun LabelDto.toEntity() =
        LabelEntity(id = id, groupId = groupId, name = name, color = color)
}
