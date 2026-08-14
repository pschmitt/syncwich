package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.ShoppingListsApi
import dev.pschmitt.syncwich.data.api.dto.CreateShoppingListItemDto
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber

/**
 * Cache-first, offline-first shopping-list access - mirrors [RecipeRepository]'s split between a
 * background-refreshed list ([refreshLists]) and lazily-refreshed-on-open detail
 * ([refreshListDetail]), see AGENTS.md's architecture section. Every read is a [Flow] from Room; a
 * failed refresh never clears cached data.
 *
 * [setItemChecked]/[syncPendingItemChecks] (SW-24/SW-33) mirror [RecipeActionRepository]'s durable
 * optimistic-update-with-retry pattern: Room is updated first so the checkbox flips immediately
 * offline, and a failed/unavailable sync leaves the item `checkedPending` for a later retry rather
 * than losing the choice. [addItem]/[removeItem] instead mirror [CookbookRepository]'s simpler
 * network-first mutation shape, like the cookbook editor's explicit save action - see
 * `ShoppingListsApi`'s kdoc for why the checked-state sync round-trips the item's full raw JSON
 * instead of sending a partial typed body.
 */
@Singleton
class ShoppingListRepository
@Inject
constructor(
    private val shoppingListsApi: ShoppingListsApi,
    private val shoppingListDao: ShoppingListDao,
) {

    fun observeLists(): Flow<List<ShoppingListEntity>> = shoppingListDao.observeLists()

    fun observeHasCachedLists(): Flow<Boolean> = observeLists().map { it.isNotEmpty() }

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

    /**
     * Fetches one list's detail (including items) and caches it. An item with a not-yet-synced
     * local checked-state change ([ShoppingListItemEntity.checkedPending]) keeps its local value
     * instead of being overwritten by the server's - same rationale as
     * `RecipeActionRepository.refreshFromServer`'s handling of `favoritePending`.
     */
    suspend fun refreshListDetail(listId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                val detail = shoppingListsApi.getShoppingListDetail(listId)
                val pendingById = shoppingListDao.getPendingCheckedItems().associateBy { it.id }
                shoppingListDao.upsertLists(listOf(detail.toEntity()))
                shoppingListDao.replaceItems(
                    listId,
                    detail.listItems.map { item ->
                        val pending = pendingById[item.id]
                        if (pending != null) {
                            item.toEntity().copy(checked = pending.checked, checkedPending = true)
                        } else {
                            item.toEntity()
                        }
                    },
                )
            }
            .onFailure {
                Timber.w(
                    it,
                    "Shopping list detail refresh failed for '$listId'; keeping cached data",
                )
            }
    }

    /**
     * Adds a freeform item to a list. Mirrors [CookbookRepository.createCookbook]'s network-first
     * shape: a failed request leaves the existing item cache untouched.
     */
    suspend fun addItem(listId: String, display: String): Result<ShoppingListItemEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                    val response =
                        shoppingListsApi.createShoppingItem(
                            CreateShoppingListItemDto(shoppingListId = listId, display = display)
                        )
                    val entity =
                        response.createdItems.firstOrNull()?.toEntity()
                            ?: error("Mealie did not return the created shopping list item")
                    shoppingListDao.upsertItems(listOf(entity))
                    entity
                }
                .onFailure { Timber.w(it, "Shopping list item create failed; keeping cached data") }
        }

    /** Deletes one item from Mealie and, only on success, removes its cached row. */
    suspend fun removeItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                shoppingListsApi.deleteShoppingItem(itemId).close()
                shoppingListDao.deleteItem(itemId)
            }
            .onFailure { Timber.w(it, "Shopping list item delete failed; keeping cached data") }
    }

    /**
     * Toggles one item's checked state offline-first: Room is updated immediately (marked
     * [ShoppingListItemEntity.checkedPending]) so the checkbox reflects the choice before any
     * network call, then a best-effort sync clears the pending flag on success or leaves it set for
     * [syncPendingItemChecks] to retry - the same shape as
     * `RecipeActionRepository.setFavorite`/`syncPendingActions`.
     */
    suspend fun setItemChecked(itemId: String, checked: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val current =
                shoppingListDao.getItem(itemId)
                    ?: return@withContext Result.failure(
                        IllegalStateException("This item is not cached on this device")
                    )
            shoppingListDao.upsertItems(
                listOf(current.copy(checked = checked, checkedPending = true))
            )
            syncItemChecked(itemId, checked)
        }

    /** Retries any durable offline checked-state changes; each item is retried independently. */
    suspend fun syncPendingItemChecks(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                val pending = shoppingListDao.getPendingCheckedItems()
                pending.forEach { item -> syncItemChecked(item.id, item.checked).getOrThrow() }
            }
            .onFailure { Timber.w(it, "Pending shopping list item sync failed; keeping pending state") }
    }

    private suspend fun syncItemChecked(itemId: String, checked: Boolean): Result<Unit> =
        runCatching {
                // Fetches the item's full current JSON representation and flips only "checked" -
                // see ShoppingListsApi's kdoc for why a partial typed body is unsafe here.
                val raw = shoppingListsApi.getShoppingItemRaw(itemId)
                val patched =
                    JsonObject(raw.toMutableMap().apply { put("checked", JsonPrimitive(checked)) })
                shoppingListsApi.updateShoppingItemRaw(itemId, patched)
                val current = shoppingListDao.getItem(itemId)
                if (current != null) {
                    shoppingListDao.upsertItems(
                        listOf(current.copy(checked = checked, checkedPending = false))
                    )
                }
            }
            .onFailure { Timber.w(it, "Shopping list item checked-state sync failed for '$itemId'") }

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
