package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.ShoppingListsApi
import dev.pschmitt.syncwich.data.api.dto.ShoppingListDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListItemDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListSummaryDto
import dev.pschmitt.syncwich.data.db.dao.ShoppingListDao
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Cache-first, offline-first shopping-list access - mirrors [RecipeRepository]'s split between a
 * background-refreshed list ([refreshLists]) and lazily-refreshed-on-open detail
 * ([refreshListDetail]), see AGENTS.md's architecture section. Every read is a [Flow] from Room; a
 * failed refresh never clears cached data. This app is read-only - there is deliberately no
 * function here to check/uncheck an item or edit a list, even though Mealie's API supports it.
 */
@Singleton
class ShoppingListRepository
@Inject
constructor(
    private val shoppingListsApi: ShoppingListsApi,
    private val shoppingListDao: ShoppingListDao,
) {

    fun observeLists(): Flow<List<ShoppingListEntity>> = shoppingListDao.observeLists()

    fun observeList(listId: String): Flow<ShoppingListEntity?> = shoppingListDao.observeList(listId)

    fun observeItems(listId: String): Flow<List<ShoppingListItemEntity>> =
        shoppingListDao.observeItems(listId)

    /**
     * Fetches every page of `/api/households/shopping/lists` and replaces the cached list-of-lists.
     * Deliberately does *not* touch any list's cached items - see
     * `ShoppingListDao.replaceAllLists`'s kdoc.
     */
    suspend fun refreshLists(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                val allItems = mutableListOf<ShoppingListSummaryDto>()
                var page = 1
                while (true) {
                    val response =
                        shoppingListsApi.getShoppingLists(
                            page = page,
                            perPage = ShoppingListsApi.DEFAULT_PAGE_SIZE,
                        )
                    allItems += response.items
                    if (response.items.isEmpty() || page >= response.totalPages) break
                    page++
                }
                shoppingListDao.replaceAllLists(allItems.map { it.toEntity() })
            }
            .onFailure { Timber.w(it, "Shopping list refresh failed; keeping cached data") }
    }

    /** Fetches one list's detail (including items) and caches it. */
    suspend fun refreshListDetail(listId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                val detail = shoppingListsApi.getShoppingListDetail(listId)
                shoppingListDao.upsertLists(listOf(detail.toEntity()))
                shoppingListDao.replaceItems(listId, detail.listItems.map { it.toEntity() })
            }
            .onFailure {
                Timber.w(
                    it,
                    "Shopping list detail refresh failed for '$listId'; keeping cached data",
                )
            }
    }

    private fun ShoppingListSummaryDto.toEntity() =
        ShoppingListEntity(id = id, name = name, updatedAt = updatedAt)

    private fun ShoppingListDto.toEntity() =
        ShoppingListEntity(id = id, name = name, updatedAt = updatedAt)

    private fun ShoppingListItemDto.toEntity() =
        ShoppingListItemEntity(
            id = id,
            shoppingListId = shoppingListId,
            display = display,
            note = note,
            checked = checked,
            position = position,
        )
}
