package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.UsersApi
import dev.pschmitt.syncwich.data.api.dto.UserDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingSummariesDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingUpdateDto
import dev.pschmitt.syncwich.data.db.dao.RecipeActionDao
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeActionRepositoryTest {

    @Test
    fun `favorite is visible and pending when network sync fails`() = runTest {
        val dao = FakeRecipeActionDao()
        val usersApi = FakeUsersApi(failure = IOException("offline"))
        val repository = RecipeActionRepository(usersApi, dao)

        assertTrue(repository.setFavorite("recipe-1", "toast", true).isFailure)

        val action = dao.get("recipe-1")!!
        assertEquals("recipe-1", action.recipeId)
        assertEquals("toast", action.recipeSlug)
        assertTrue(action.isFavorite)
        assertTrue(action.favoritePending)
    }

    @Test
    fun `pending favorite is synchronized and cleared later`() = runTest {
        val dao =
            FakeRecipeActionDao(
                seed =
                    RecipeActionEntity(
                        recipeId = "recipe-1",
                        recipeSlug = "toast",
                        isFavorite = true,
                        favoritePending = true,
                    )
            )
        val usersApi = FakeUsersApi()
        val repository = RecipeActionRepository(usersApi, dao)

        assertTrue(repository.syncPendingActions().isSuccess)

        assertFalse(dao.get("recipe-1")!!.favoritePending)
        assertEquals(1, usersApi.addFavoriteCalls)
    }

    @Test
    fun `rating is visible and pending when network sync fails`() = runTest {
        val dao = FakeRecipeActionDao()
        val repository =
            RecipeActionRepository(
                FakeUsersApi(failure = IOException("offline")),
                dao,
            )

        assertTrue(repository.setRating("recipe-1", "toast", 4).isFailure)

        val action = dao.get("recipe-1")!!
        assertEquals(4, action.rating)
        assertTrue(action.ratingPending)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rating outside the five star range is rejected before network`() = runTest {
        RecipeActionRepository(FakeUsersApi(), FakeRecipeActionDao())
            .setRating("recipe-1", "toast", 6)
    }

    private class FakeRecipeActionDao(seed: RecipeActionEntity? = null) : RecipeActionDao {
        private val state = MutableStateFlow(seed?.let { mapOf(it.recipeId to it) } ?: emptyMap())

        override fun observe(recipeId: String): Flow<RecipeActionEntity?> = state.map {
            it[recipeId]
        }

        override fun observeFavoriteIds(): Flow<List<String>> = state.map { actions ->
            actions.values.filter { it.isFavorite }.map { it.recipeId }.sorted()
        }

        override suspend fun get(recipeId: String): RecipeActionEntity? = state.value[recipeId]

        override suspend fun getAll(): List<RecipeActionEntity> = state.value.values.toList()

        override suspend fun getPending(): List<RecipeActionEntity> =
            state.value.values.filter { it.favoritePending || it.ratingPending }

        override suspend fun upsert(action: RecipeActionEntity) {
            state.value = state.value + (action.recipeId to action)
        }

        override suspend fun upsertAll(actions: List<RecipeActionEntity>) {
            actions.forEach { upsert(it) }
        }

        override suspend fun delete(recipeId: String) {
            state.value = state.value - recipeId
        }
    }

    private class FakeUsersApi(private val failure: Throwable? = null) : UsersApi {
        var addFavoriteCalls = 0

        override suspend fun getSelf(): UserDto {
            failure?.let { throw it }
            return UserDto(id = "user-1")
        }

        override suspend fun getSelfFavorites(): UserRatingSummariesDto =
            failure?.let { throw it } ?: UserRatingSummariesDto(emptyList())

        override suspend fun getSelfRatings(): UserRatingSummariesDto =
            failure?.let { throw it } ?: UserRatingSummariesDto(emptyList())

        override suspend fun addFavorite(userId: String, recipeSlug: String): ResponseBody {
            failure?.let { throw it }
            addFavoriteCalls++
            return "".toResponseBody()
        }

        override suspend fun removeFavorite(userId: String, recipeSlug: String): ResponseBody {
            failure?.let { throw it }
            return "".toResponseBody()
        }

        override suspend fun updateRating(
            userId: String,
            recipeSlug: String,
            request: UserRatingUpdateDto,
        ): ResponseBody {
            failure?.let { throw it }
            return "".toResponseBody()
        }
    }
}
