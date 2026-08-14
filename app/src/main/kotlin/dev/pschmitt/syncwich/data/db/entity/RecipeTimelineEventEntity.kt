package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of one recipe's cooking-event ("I made this") timeline. Keyed by [localId] - a
 * client-generated id for a not-yet-synced entry (so an offline "I made this" tap is durable before
 * the create POST ever succeeds), or the server's own event id once known. Mirrors
 * [RecipeActionEntity]'s pending-sync pattern: [pending] keeps a row queued for
 * [dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository.syncPendingEvents], and is only
 * cleared once the create request actually succeeds.
 */
@Entity(tableName = "recipe_timeline_events")
data class RecipeTimelineEventEntity(
    @PrimaryKey val localId: String,
    val recipeId: String,
    val subject: String,
    val eventType: String = "comment",
    val eventMessage: String? = null,
    val timestamp: Long = 0L,
    val pending: Boolean = false,
    val updatedAt: Long = 0L,
)
