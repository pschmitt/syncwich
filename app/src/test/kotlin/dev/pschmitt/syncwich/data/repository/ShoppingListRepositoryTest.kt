package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.ShoppingListsApi
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListItemDto
import dev.pschmitt.syncwich.data.api.dto.ShoppingListSummaryDto
import dev.pschmitt.syncwich.data.db.dao.ShoppingListDao
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import java.io.IOException
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
 * never clear or block what's already cached. Also verifies a [ShoppingListRepository]-specific
 * subtlety: refreshing the list-of-lists must not wipe out a list's already-cached items (fetched
 * separately, on-demand, by [ShoppingListRepository.refreshListDetail]) - see
 * `ShoppingListDao.replaceAllLists`'s kdoc.
 */
class ShoppingListRepositoryTest {

    @Test
    fun `refreshLists replaces the cached list-of-lists on success`() = runTest {
        val dao = FakeShoppingListDao(lists = listOf(ShoppingListEntity("old-1", "Old", null)))
        val api =
            FakeShoppingListsApi(lists = listOf(ShoppingListSummaryDto("new-1", "New", null, null)))
        val repository = ShoppingListRepository(api, dao)

        val result = repository.refreshLists()

        assertTrue(result.isSuccess)
        assertEquals(listOf("New"), dao.observeLists().first().map { it.name })
    }

    @Test
    fun `a failed refreshLists leaves the cache untouched`() = runTest {
        val cached = listOf(ShoppingListEntity("keep-1", "Keep Me", null))
        val dao = FakeShoppingListDao(lists = cached)
        val api = FakeShoppingListsApi(failure = IOException("network down"))
        val repository = ShoppingListRepository(api, dao)

        val result = repository.refreshLists()

        assertTrue(result.isFailure)
        assertEquals(cached, dao.observeLists().first())
    }

    @Test
    fun `refreshLists does not wipe a still-present list's cached items`() = runTest {
        val dao =
            FakeShoppingListDao(
                lists = listOf(ShoppingListEntity("list-1", "Groceries", null)),
                items = listOf(ShoppingListItemEntity("item-1", "list-1", "Milk", null, false, 0)),
            )
        val api =
            FakeShoppingListsApi(
                lists = listOf(ShoppingListSummaryDto("list-1", "Groceries", null, "2026-08-14"))
            )
        val repository = ShoppingListRepository(api, dao)

        val result = repository.refreshLists()

        assertTrue(result.isSuccess)
        assertEquals(1, dao.observeItems("list-1").first().size)
    }

    @Test
    fun `refreshListDetail replaces a list's items on success`() = runTest {
        val dao =
            FakeShoppingListDao(
                lists = listOf(ShoppingListEntity("list-1", "Groceries", null)),
                items =
                    listOf(ShoppingListItemEntity("stale-1", "list-1", "Stale", null, false, 0)),
            )
        val api =
            FakeShoppingListsApi(
                detail =
                    ShoppingListDto(
                        id = "list-1",
                        name = "Groceries",
                        listItems =
                            listOf(
                                ShoppingListItemDto(
                                    id = "item-1",
                                    shoppingListId = "list-1",
                                    display = "Milk",
                                    checked = true,
                                )
                            ),
                    )
            )
        val repository = ShoppingListRepository(api, dao)

        val result = repository.refreshListDetail("list-1")

        assertTrue(result.isSuccess)
        val items = dao.observeItems("list-1").first()
        assertEquals(listOf("Milk"), items.map { it.display })
        assertTrue(items.single().checked)
    }

    @Test
    fun `a failed refreshListDetail leaves cached items untouched`() = runTest {
        val cachedItems = listOf(ShoppingListItemEntity("item-1", "list-1", "Milk", null, false, 0))
        val dao =
            FakeShoppingListDao(
                lists = listOf(ShoppingListEntity("list-1", "Groceries", null)),
                items = cachedItems,
            )
        val api = FakeShoppingListsApi(failure = IOException("network down"))
        val repository = ShoppingListRepository(api, dao)

        val result = repository.refreshListDetail("list-1")

        assertTrue(result.isFailure)
        assertEquals(cachedItems, dao.observeItems("list-1").first())
    }

    private class FakeShoppingListDao(
        lists: List<ShoppingListEntity> = emptyList(),
        items: List<ShoppingListItemEntity> = emptyList(),
    ) : ShoppingListDao {
        private val listsState = MutableStateFlow(lists)
        private val itemsState = MutableStateFlow(items)

        override fun observeLists(): Flow<List<ShoppingListEntity>> = listsState

        override fun observeList(listId: String): Flow<ShoppingListEntity?> = listsState.map {
            it.find { list -> list.id == listId }
        }

        override fun observeItems(listId: String): Flow<List<ShoppingListItemEntity>> =
            itemsState.map {
                it.filter { item -> item.shoppingListId == listId }
            }

        override suspend fun upsertLists(lists: List<ShoppingListEntity>) {
            val byId = listsState.value.associateBy { it.id }.toMutableMap()
            lists.forEach { byId[it.id] = it }
            listsState.value = byId.values.toList()
        }

        override suspend fun upsertItems(items: List<ShoppingListItemEntity>) {
            val byId = itemsState.value.associateBy { it.id }.toMutableMap()
            items.forEach { byId[it.id] = it }
            itemsState.value = byId.values.toList()
        }

        override suspend fun deleteAllLists() {
            listsState.value = emptyList()
        }

        override suspend fun deleteListsNotIn(keepIds: List<String>) {
            listsState.value = listsState.value.filter { it.id in keepIds }
        }

        override suspend fun deleteItems(listId: String) {
            itemsState.value = itemsState.value.filterNot { it.shoppingListId == listId }
        }
    }

    private class FakeShoppingListsApi(
        private val lists: List<ShoppingListSummaryDto> = emptyList(),
        private val detail: ShoppingListDto? = null,
        private val failure: Throwable? = null,
    ) : ShoppingListsApi {
        override suspend fun getShoppingLists(
            page: Int,
            perPage: Int,
        ): PagedResponseDto<ShoppingListSummaryDto> {
            failure?.let { throw it }
            return PagedResponseDto(1, lists.size, lists.size, 1, lists)
        }

        override suspend fun getShoppingListDetail(id: String): ShoppingListDto {
            failure?.let { throw it }
            return detail ?: error("no detail configured")
        }
    }
}
