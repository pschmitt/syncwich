package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.ShoppingListEntity
import dev.pschmitt.syncwich.data.db.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_lists ORDER BY name COLLATE NOCASE ASC")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    fun observeList(listId: String): Flow<ShoppingListEntity?>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY position ASC")
    fun observeItems(listId: String): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE id = :itemId")
    suspend fun getItem(itemId: String): ShoppingListItemEntity?

    @Query("SELECT * FROM shopping_list_items WHERE checkedPending = 1")
    suspend fun getPendingCheckedItems(): List<ShoppingListItemEntity>

    @Upsert suspend fun upsertLists(lists: List<ShoppingListEntity>)

    @Upsert suspend fun upsertItems(items: List<ShoppingListItemEntity>)

    @Query("DELETE FROM shopping_lists") suspend fun deleteAllLists()

    @Query("DELETE FROM shopping_lists WHERE id NOT IN (:keepIds)")
    suspend fun deleteListsNotIn(keepIds: List<String>)

    @Query("DELETE FROM shopping_list_items WHERE shoppingListId = :listId")
    suspend fun deleteItems(listId: String)

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    /**
     * Upserts the current list-of-lists and removes only lists no longer returned by the server -
     * deliberately not a delete-all-then-reinsert like `CategoryDao.replaceAll`, because
     * [ShoppingListItemEntity] cascades off a list's id: wiping every list row on every refresh
     * would also wipe every list's cached items (fetched separately by [replaceItems], on-demand,
     * like `RecipeDetailEntity`) even though nothing about them changed.
     */
    @Transaction
    suspend fun replaceAllLists(lists: List<ShoppingListEntity>) {
        if (lists.isEmpty()) {
            deleteAllLists()
        } else {
            deleteListsNotIn(lists.map { it.id })
        }
        upsertLists(lists)
    }

    /** Atomically replaces one list's items, e.g. after that list's detail is re-fetched. */
    @Transaction
    suspend fun replaceItems(listId: String, items: List<ShoppingListItemEntity>) {
        deleteItems(listId)
        upsertItems(items)
    }
}
