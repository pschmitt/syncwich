package dev.pschmitt.syncwich.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pschmitt.syncwich.data.repository.CategoryRepository
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.TagRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import timber.log.Timber

/**
 * Refreshes the offline recipe cache in the background: recipe list, category/tag dictionaries, and
 * cookbooks (plus each cookbook's matching recipes). Never wipes or blocks what's already cached on
 * failure - each repository's own `refresh*`
 * function already logs and swallows its own errors (see [RecipeRepository.refreshRecipes]'s kdoc),
 * so a bad run here only means "still showing what was last cached", never a blank screen.
 *
 * Deliberately does *not* bulk-fetch every recipe's full detail - that's comparatively expensive
 * (one request per recipe) and detail is instead refreshed lazily by
 * [RecipeRepository.refreshRecipeDetail] whenever a recipe is actually opened.
 */
@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val cookbookRepository: CookbookRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.isConfigured) return Result.success()

        val failures =
            listOf(
                    recipeRepository.refreshRecipes(),
                    categoryRepository.refreshCategories(),
                    tagRepository.refreshTags(),
                    cookbookRepository.refreshCookbooks(),
                )
                .mapNotNull { it.exceptionOrNull() }

        return if (failures.isEmpty()) {
            settingsRepository.recordSyncSuccess()
            Result.success()
        } else {
            val message = failures.first().message?.takeIf { it.isNotBlank() } ?: "Sync failed"
            settingsRepository.recordSyncFailure(message)
            Timber.w("Sync completed with ${failures.size} failure(s): $message")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
