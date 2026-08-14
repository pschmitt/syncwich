package dev.pschmitt.syncwich.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookbookDao {

    @Query("SELECT * FROM cookbooks ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CookbookEntity>>

    @Query("SELECT * FROM cookbooks WHERE id = :id")
    fun observeById(id: String): Flow<CookbookEntity?>

    @Query("SELECT * FROM cookbooks WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<CookbookEntity?>

    @Query(
        """
        SELECT cookbooks.* FROM cookbooks
        INNER JOIN recipe_cookbook_cross_refs
            ON cookbooks.id = recipe_cookbook_cross_refs.cookbookId
        WHERE recipe_cookbook_cross_refs.recipeId = :recipeId
        ORDER BY cookbooks.position ASC, cookbooks.name COLLATE NOCASE ASC
        """
    )
    fun observeByRecipe(recipeId: String): Flow<List<CookbookEntity>>

    @Upsert suspend fun upsertAll(cookbooks: List<CookbookEntity>)

    @Query("DELETE FROM cookbooks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cookbooks") suspend fun deleteAll()

    /** Atomically replaces the whole cookbook dictionary - see [CategoryDao.replaceAll]'s kdoc. */
    @Transaction
    suspend fun replaceAll(cookbooks: List<CookbookEntity>) {
        deleteAll()
        upsertAll(cookbooks)
    }
}
