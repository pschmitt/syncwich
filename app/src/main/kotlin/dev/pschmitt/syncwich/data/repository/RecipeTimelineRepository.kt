package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.TimelineApi
import dev.pschmitt.syncwich.data.api.UsersApi
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventDto
import dev.pschmitt.syncwich.data.api.dto.RecipeTimelineEventInDto
import dev.pschmitt.syncwich.data.db.dao.RecipeTimelineEventDao
import dev.pschmitt.syncwich.data.db.entity.RecipeTimelineEventEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Offline-first "I made this" cooking-event timeline for a recipe.
 *
 * Confirmed live against a real v3.22.0 Mealie instance: `GET /api/recipes/timeline/events` with a
 * `queryFilter=recipeId="<uuid>"` query returned that recipe's real history, mixing Mealie's own
 * `system`-type "Recipe Created" entries with `comment`-type "<user's full name> made this" entries
 * recorded by Mealie's official clients (empty `eventMessage`, not null). [recordMadeThis] mirrors
 * that exact shape.
 *
 * The create endpoint (`POST /api/recipes/timeline/events`) was **not** exercised with a live
 * write - per this task's hard rule against unapproved live writes against the verification
 * account - so its request/response handling here is built from the public schema
 * (`RecipeTimelineEventIn`/`RecipeTimelineEventOut`) plus the real event shapes read back above,
 * not from an actual successful POST. Treat the write path as schema-verified, not live-verified,
 * until a real device test confirms it end to end.
 *
 * Mirrors [RecipeActionRepository]'s durable pending-sync pattern: a "I made this" tap is written
 * to Room immediately (so it survives process death or being offline) and only leaves the pending
 * queue once the create request actually succeeds - at which point the local placeholder row is
 * replaced by the server's own event id so a later [refreshFromServer] can't duplicate it.
 */
@Singleton
class RecipeTimelineRepository
@Inject
constructor(
    private val timelineApi: TimelineApi,
    private val usersApi: UsersApi,
    private val recipeTimelineEventDao: RecipeTimelineEventDao,
) {

    fun observe(recipeId: String): Flow<List<RecipeTimelineEventEntity>> =
        recipeTimelineEventDao.observeForRecipe(recipeId)

    /**
     * Records a durable "I made this" event, retried later by [syncPendingEvents] on failure.
     *
     * The local row is written with a generic [PENDING_SUBJECT] placeholder rather than the user's
     * real display name - resolving that name is itself a network call (`GET /api/users/self`), and
     * the whole point of this being offline-safe is that the local save must never depend on
     * connectivity. The real "<name> made this" subject is only resolved inside [syncEvent], once a
     * network call is already required anyway; the local placeholder is replaced by the server's
     * own event (with its real subject) as soon as that sync succeeds.
     */
    suspend fun recordMadeThis(recipeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val event =
                RecipeTimelineEventEntity(
                    localId = UUID.randomUUID().toString(),
                    recipeId = recipeId,
                    subject = PENDING_SUBJECT,
                    eventType = "comment",
                    eventMessage = "",
                    timestamp = System.currentTimeMillis(),
                    pending = true,
                    updatedAt = System.currentTimeMillis(),
                )
            recipeTimelineEventDao.upsert(event)
            syncEvent(event)
        }

    /**
     * Cache-first read for the timeline screen: replaces previously-synced rows for this recipe
     * with the server's current history while leaving any not-yet-synced pending rows untouched
     * (they have their own client-generated [RecipeTimelineEventEntity.localId] and so can never
     * collide with a server-assigned one).
     */
    suspend fun refreshFromServer(recipeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = timelineApi.getEvents(queryFilter = recipeIdFilter(recipeId))
                recipeTimelineEventDao.upsertAll(response.items.map { it.toEntity() })
            }
                .onFailure { Timber.w(it, "Timeline refresh failed; keeping cached events") }
        }

    /** Retries durable offline "I made this" taps that haven't reached the server yet. */
    suspend fun syncPendingEvents(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                recipeTimelineEventDao.getPending().forEach { event ->
                    syncEvent(event).getOrThrow()
                }
            }
                .onFailure {
                    Timber.w(it, "Pending timeline event sync failed; keeping pending events")
                }
        }

    private suspend fun syncEvent(event: RecipeTimelineEventEntity): Result<Unit> = runCatching {
        val subject =
            if (event.subject == PENDING_SUBJECT) "${displayName()} made this" else event.subject
        val created =
            timelineApi.createEvent(
                RecipeTimelineEventInDto(
                    recipeId = event.recipeId,
                    subject = subject,
                    eventType = event.eventType,
                    eventMessage = event.eventMessage,
                    timestamp = Instant.ofEpochMilli(event.timestamp).toString(),
                )
            )
        // Swap the client-generated placeholder for the server's own id so a later
        // refreshFromServer upserts onto the same row instead of duplicating it.
        recipeTimelineEventDao.delete(event.localId)
        recipeTimelineEventDao.upsert(created.toEntity())
    }
        .onFailure { Timber.w(it, "Timeline event create failed; keeping it pending") }

    private suspend fun displayName(): String {
        val self = usersApi.getSelf()
        return self.fullName?.takeIf { it.isNotBlank() }
            ?: self.username?.takeIf { it.isNotBlank() }
            ?: "You"
    }

    private fun recipeIdFilter(recipeId: String): String {
        val escaped = recipeId.replace("\"", "\\\"")
        return "recipeId=\"$escaped\""
    }

    private fun RecipeTimelineEventDto.toEntity() =
        RecipeTimelineEventEntity(
            localId = id,
            recipeId = recipeId,
            subject = subject,
            eventType = eventType,
            eventMessage = eventMessage,
            timestamp = runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrDefault(0L),
            pending = false,
            updatedAt = System.currentTimeMillis(),
        )

    private companion object {
        /**
         * Offline-safe placeholder subject for a not-yet-synced "I made this" row - see
         * [recordMadeThis]'s kdoc for why the real display name isn't resolved until sync time.
         */
        const val PENDING_SUBJECT = "You made this"
    }
}
