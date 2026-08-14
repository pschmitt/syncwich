package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.OrganizersApi
import dev.pschmitt.syncwich.data.api.dto.OrganizerDto
import dev.pschmitt.syncwich.data.db.dao.TagDao
import dev.pschmitt.syncwich.data.db.entity.TagEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/** Cache-first, offline-first tag access - mirrors [CategoryRepository]'s shape exactly. */
@Singleton
class TagRepository
@Inject
constructor(private val organizersApi: OrganizersApi, private val tagDao: TagDao) {

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun refreshTags(): Result<Unit> =
        runCatching {
                val allItems = mutableListOf<OrganizerDto>()
                var page = 1
                while (true) {
                    val response =
                        organizersApi.getTags(
                            page = page,
                            perPage = OrganizersApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                tagDao.replaceAll(allItems.map { it.toEntity() })
            }
            .onFailure { Timber.w(it, "Tag refresh failed; keeping cached data") }

    private fun OrganizerDto.toEntity() = TagEntity(id = id, name = name, slug = slug)
}
