package dev.pschmitt.syncwich.data.repository

import dev.pschmitt.syncwich.data.api.UsersApi
import dev.pschmitt.syncwich.data.api.dto.UserRatingUpdateDto
import dev.pschmitt.syncwich.data.db.dao.RecipeActionDao
import dev.pschmitt.syncwich.data.db.entity.RecipeActionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Offline-first favorite and rating state for recipe detail actions.
 *
 * Room is updated before any network call. A failed or unavailable Mealie request therefore leaves
 * the user's choice visible offline and marks it pending for [syncPendingActions]. The repository
 * intentionally has no UI side effects and never attempts a write during a read/refresh path.
 */
@Singleton
class RecipeActionRepository
@Inject
constructor(
    private val usersApi: UsersApi,
    private val recipeActionDao: RecipeActionDao,
) {

    fun observe(recipeId: String): Flow<RecipeActionEntity?> = recipeActionDao.observe(recipeId)

    suspend fun setFavorite(
        recipeId: String,
        recipeSlug: String,
        isFavorite: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val action =
                saveLocal(
                    recipeId = recipeId,
                    recipeSlug = recipeSlug,
                    update = { it.copy(isFavorite = isFavorite, favoritePending = true) },
                )
            syncFavorite(action)
        }

    /** A null rating clears the server rating; non-null values are restricted to the UI's 1..5. */
    suspend fun setRating(recipeId: String, recipeSlug: String, rating: Int?): Result<Unit> =
        withContext(Dispatchers.IO) {
            require(rating == null || rating in 1..5) { "Recipe rating must be between 1 and 5" }
            val action =
                saveLocal(
                    recipeId = recipeId,
                    recipeSlug = recipeSlug,
                    update = { it.copy(rating = rating, ratingPending = true) },
                )
            syncRating(action)
        }

    /**
     * Imports the two read-only self collections without overwriting a local pending choice. This
     * is safe to call from a normal refresh because it does not issue any write request.
     */
    suspend fun refreshFromServer(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val favorites = usersApi.getSelfFavorites().ratings
                val ratings = usersApi.getSelfRatings().ratings
                val remoteByRecipe =
                    (ratings + favorites.map { it.copy(isFavorite = true) }).associateBy {
                        it.recipeId
                    }
                val favoriteIds = favorites.mapTo(mutableSetOf()) { it.recipeId }
                val existing = recipeActionDao.getAll().associateBy { it.recipeId }
                val allRecipeIds = existing.keys + remoteByRecipe.keys
                recipeActionDao.upsertAll(
                    allRecipeIds.map { recipeId ->
                        val local = existing[recipeId]
                        val remote = remoteByRecipe[recipeId]
                        RecipeActionEntity(
                            recipeId = recipeId,
                            recipeSlug = local?.recipeSlug.orEmpty(),
                            isFavorite =
                                if (local?.favoritePending == true) local.isFavorite
                                else recipeId in favoriteIds,
                            rating =
                                if (local?.ratingPending == true) local.rating
                                else remote?.rating?.toInt(),
                            favoritePending = local?.favoritePending ?: false,
                            ratingPending = local?.ratingPending ?: false,
                            updatedAt = local?.updatedAt ?: System.currentTimeMillis(),
                        )
                    }
                )
            }
                .onFailure { Timber.w(it, "Recipe action refresh failed; keeping cached actions") }
        }

    /** Retries durable offline choices; each successful flag is cleared independently. */
    suspend fun syncPendingActions(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pending = recipeActionDao.getPending()
                if (pending.isEmpty()) return@runCatching
                val userId = requireUserId()
                pending.forEach { action -> syncPendingAction(action, userId).getOrThrow() }
            }
                .onFailure {
                    Timber.w(it, "Pending recipe action sync failed; keeping pending choices")
                }
        }

    private suspend fun saveLocal(
        recipeId: String,
        recipeSlug: String,
        update: (RecipeActionEntity) -> RecipeActionEntity,
    ): RecipeActionEntity {
        val current =
            recipeActionDao.get(recipeId)
                ?: RecipeActionEntity(recipeId = recipeId, recipeSlug = recipeSlug)
        return update(current.copy(recipeSlug = recipeSlug, updatedAt = System.currentTimeMillis()))
            .also { recipeActionDao.upsert(it) }
    }

    private suspend fun syncFavorite(action: RecipeActionEntity): Result<Unit> = runCatching {
        val userId = requireUserId()
        val response =
            if (action.isFavorite) usersApi.addFavorite(userId, action.recipeSlug)
            else usersApi.removeFavorite(userId, action.recipeSlug)
        response.close()
        recipeActionDao.upsert(
            action.copy(favoritePending = false, updatedAt = System.currentTimeMillis())
        )
    }
        .onFailure { Timber.w(it, "Favorite sync failed; keeping the local choice") }

    private suspend fun syncRating(action: RecipeActionEntity): Result<Unit> = runCatching {
        val userId = requireUserId()
        val response =
            usersApi.updateRating(
                userId,
                action.recipeSlug,
                UserRatingUpdateDto.forRating(action.rating),
            )
        response.close()
        recipeActionDao.upsert(
            action.copy(ratingPending = false, updatedAt = System.currentTimeMillis())
        )
    }
        .onFailure { Timber.w(it, "Rating sync failed; keeping the local choice") }

    private suspend fun syncPendingAction(
        action: RecipeActionEntity,
        userId: String,
    ): Result<Unit> = runCatching {
        var current = action
        if (current.favoritePending) {
            val response =
                if (current.isFavorite) usersApi.addFavorite(userId, current.recipeSlug)
                else usersApi.removeFavorite(userId, current.recipeSlug)
            response.close()
            current = current.copy(favoritePending = false)
            recipeActionDao.upsert(current)
        }
        if (current.ratingPending) {
            val response =
                usersApi.updateRating(
                    userId,
                    current.recipeSlug,
                    UserRatingUpdateDto.forRating(current.rating),
                )
            response.close()
            current = current.copy(ratingPending = false)
            recipeActionDao.upsert(current)
        }
    }
        .onFailure { Timber.w(it, "Recipe action sync failed for '${action.recipeSlug}'") }

    private suspend fun requireUserId(): String =
        usersApi.getSelf().id?.takeIf { it.isNotBlank() }
            ?: error("Mealie did not return the authenticated user's id")
}
