package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.TimelineApi
import dev.pschmitt.syncwich.data.api.UsersApi
import dev.pschmitt.syncwich.data.api.dto.PagedResponseDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventInDto
import dev.pschmitt.syncwich.data.api.dto.UserDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingSummariesDto
import dev.pschmitt.syncwich.data.api.dto.UserRatingUpdateDto
import dev.pschmitt.syncwich.data.db.dao.RecipeTimelineEventDao
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
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

class RecipeTimelineRepositoryTest {

    @Test
    fun `made-this event is durable and pending when the network is unavailable`() = runTest {
        val dao = FakeRecipeTimelineEventDao()
        val repository =
            RecipeTimelineRepository(
                FakeTimelineApi(failure = IOException("offline")),
                FakeUsersApi(),
                dao,
            )

        assertTrue(repository.recordMadeThis("recipe-1").isFailure)

        val events = dao.getForRecipe("recipe-1")
        assertEquals(1, events.size)
        assertTrue(events.single().pending)
        // The offline-safe placeholder subject never requires a network call to resolve.
        assertEquals("You made this", events.single().subject)
    }

    @Test
    fun `made-this event resolves the real display name and clears pending once synced`() =
        runTest {
            val dao = FakeRecipeTimelineEventDao()
            val usersApi = FakeUsersApi(fullName = "Ada Lovelace")
            val timelineApi = FakeTimelineApi()
            val repository = RecipeTimelineRepository(timelineApi, usersApi, dao)

            assertTrue(repository.recordMadeThis("recipe-1").isSuccess)

            val events = dao.getForRecipe("recipe-1")
            assertEquals(1, events.size)
            val event = events.single()
            assertFalse(event.pending)
            assertEquals("Ada Lovelace made this", event.subject)
            assertEquals("server-1", event.localId)
            assertEquals(1, timelineApi.createEventCalls)
        }

    @Test
    fun `pending events are retried and cleared by syncPendingEvents`() = runTest {
        val dao =
            FakeRecipeTimelineEventDao(
                seed =
                    RecipeTimelineEventEntity(
                        localId = "local-1",
                        recipeId = "recipe-1",
                        subject = "You made this",
                        pending = true,
                    )
            )
        val repository = RecipeTimelineRepository(FakeTimelineApi(), FakeUsersApi(), dao)

        assertTrue(repository.syncPendingEvents().isSuccess)

        assertTrue(dao.getPending().isEmpty())
        assertEquals(1, dao.getForRecipe("recipe-1").size)
        assertFalse(dao.getForRecipe("recipe-1").single().pending)
    }

    private class FakeRecipeTimelineEventDao(seed: RecipeTimelineEventEntity? = null) :
        RecipeTimelineEventDao {
        private val state = MutableStateFlow(seed?.let { mapOf(it.localId to it) } ?: emptyMap())

        override fun observeForRecipe(recipeId: String): Flow<List<RecipeTimelineEventEntity>> =
            state.map { events ->
                events.values.filter { it.recipeId == recipeId }
            }

        override suspend fun getForRecipe(recipeId: String): List<RecipeTimelineEventEntity> =
            state.value.values.filter { it.recipeId == recipeId }

        override suspend fun getPending(): List<RecipeTimelineEventEntity> =
            state.value.values.filter { it.pending }

        override suspend fun upsert(event: RecipeTimelineEventEntity) {
            state.value = state.value + (event.localId to event)
        }

        override suspend fun upsertAll(events: List<RecipeTimelineEventEntity>) {
            events.forEach { upsert(it) }
        }

        override suspend fun delete(localId: String) {
            state.value = state.value - localId
        }
    }

    private class FakeTimelineApi(private val failure: Throwable? = null) : TimelineApi {
        var createEventCalls = 0

        override suspend fun getEvents(
            queryFilter: String,
            orderDirection: String,
            page: Int,
            perPage: Int,
        ): PagedResponseDto<RecipeTimelineEventDto> {
            failure?.let { throw it }
            return PagedResponseDto(
                page = 1,
                perPage = perPage,
                total = 0,
                totalPages = 0,
                items = emptyList(),
            )
        }

        override suspend fun createEvent(
            request: RecipeTimelineEventInDto
        ): RecipeTimelineEventDto {
            failure?.let { throw it }
            createEventCalls++
            return RecipeTimelineEventDto(
                id = "server-1",
                recipeId = request.recipeId,
                subject = request.subject,
                eventType = request.eventType,
                eventMessage = request.eventMessage,
                timestamp = request.timestamp,
            )
        }
    }

    private class FakeUsersApi(private val fullName: String? = null) : UsersApi {
        override suspend fun getSelf(): UserDto = UserDto(id = "user-1", fullName = fullName)

        override suspend fun getSelfFavorites(): UserRatingSummariesDto =
            UserRatingSummariesDto(emptyList())

        override suspend fun getSelfRatings(): UserRatingSummariesDto =
            UserRatingSummariesDto(emptyList())

        override suspend fun addFavorite(userId: String, recipeSlug: String): ResponseBody =
            "".toResponseBody()

        override suspend fun removeFavorite(userId: String, recipeSlug: String): ResponseBody =
            "".toResponseBody()

        override suspend fun updateRating(
            userId: String,
            recipeSlug: String,
            request: UserRatingUpdateDto,
        ): ResponseBody = "".toResponseBody()
    }
}
