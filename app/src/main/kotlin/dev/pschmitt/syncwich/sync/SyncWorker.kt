package dev.pschmitt.syncwich.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pschmitt.syncwich.data.onboarding.OidcAuthClient
import dev.pschmitt.syncwich.data.repository.CategoryRepository
import dev.pschmitt.syncwich.data.repository.CookbookRepository
import dev.pschmitt.syncwich.data.repository.FoodRepository
import dev.pschmitt.syncwich.data.repository.MealPlanRepository
import dev.pschmitt.syncwich.data.repository.RecipeActionRepository
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.repository.RecipeTimelineRepository
import dev.pschmitt.syncwich.data.repository.ShoppingListRepository
import dev.pschmitt.syncwich.data.repository.TagRepository
import dev.pschmitt.syncwich.data.repository.ToolRepository
import dev.pschmitt.syncwich.data.settings.MealieAuthMethod
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Refreshes the offline recipe cache in the background: recipe list, category/tag/food/tool
 * dictionaries, cookbooks (plus each cookbook's matching recipes), the shopping list-of-lists, a
 * rolling meal-plan window, and best-effort image prefetching from the existing Room cache. Never
 * wipes or blocks what's already cached on failure - each repository's own `refresh*` function
 * already logs and swallows its own errors (see [RecipeRepository.refreshRecipes]'s kdoc), so a bad
 * run here only means "still showing what's last cached", never a blank screen.
 *
 * Deliberately does *not* bulk-fetch every recipe's full detail - that's comparatively expensive
 * (one request per recipe) and detail is instead refreshed lazily by
 * [RecipeRepository.refreshRecipeDetail] whenever a recipe is actually opened. Image prefetching
 * does not change that: it only examines detail JSON that is already cached.
 */
@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recipeRepository: RecipeRepository,
    private val recipeActionRepository: RecipeActionRepository,
    private val recipeTimelineRepository: RecipeTimelineRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val foodRepository: FoodRepository,
    private val toolRepository: ToolRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val cookbookRepository: CookbookRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val oidcAuthClient: OidcAuthClient,
    private val settingsRepository: SettingsRepository,
    private val recipeImagePrefetcher: RecipeImagePrefetcher,
    private val syncNotifier: SyncNotifier,
    private val syncStatusRepository: SyncStatusRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.isConfigured) return Result.success()
        syncNotifier.notifySyncStarted()

        // Covers the current week plus the next two, matching how far a user is likely to page
        // forward in MealPlanScreen before going offline - a background job can't know which week
        // they'll actually look at, unlike the on-open refresh in MealPlanViewModel.
        val mealPlanStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val mealPlanEnd = mealPlanStart.plusWeeks(3).minusDays(1)

        val failures =
            listOf(
                    syncStep("Refreshing OIDC session…") { refreshOidcSession() },
                    syncStep("Refreshing recipes…") {
                        recipeRepository.refreshRecipes(forceRefresh = false)
                    },
                    // No request is made when there are no pending offline actions; pending
                    // favorite/rating flags are retried only after Room has made them visible.
                    syncStep("Syncing recipe actions…") {
                        recipeActionRepository.syncPendingActions()
                    },
                    // Retries any “I made this” taps recorded while offline.
                    syncStep("Syncing cooking events…") {
                        recipeTimelineRepository.syncPendingEvents()
                    },
                    syncStep("Refreshing categories…") { categoryRepository.refreshCategories() },
                    syncStep("Refreshing tags…") { tagRepository.refreshTags() },
                    syncStep("Refreshing foods…") { foodRepository.refreshFoods() },
                    syncStep("Refreshing tools…") { toolRepository.refreshTools() },
                    syncStep("Refreshing shopping lists…") {
                        shoppingListRepository.refreshLists()
                    },
                    // No request is made when there are no pending offline checked-state changes;
                    // mirrors recipeActionRepository.syncPendingActions() above.
                    syncStep("Syncing shopping-list checks…") {
                        shoppingListRepository.syncPendingItemChecks()
                    },
                    syncStep("Refreshing cookbooks…") {
                        cookbookRepository.refreshCookbooks(forceRefresh = false)
                    },
                    syncStep("Refreshing meal plan…") {
                        mealPlanRepository.refreshMealPlan(mealPlanStart, mealPlanEnd)
                    },
                )
                .mapNotNull { it.exceptionOrNull() }

        try {
            recipeImagePrefetcher.prefetchCachedRecipeImages(
                settingsRepository.credentials.value.serverUrl
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Timber.w(failure, "Recipe image prefetch pass failed")
        }

        return if (failures.isEmpty()) {
            settingsRepository.recordSyncSuccess()
            syncStatusRepository.clearProgress()
            syncNotifier.notifySyncSucceeded()
            Result.success()
        } else {
            val message = failures.first().message?.takeIf { it.isNotBlank() } ?: "Sync failed"
            settingsRepository.recordSyncFailure(message)
            Timber.w("Sync completed with ${failures.size} failure(s): $message")
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                syncNotifier.notifySyncRetry(runAttemptCount + 1)
                Result.retry()
            } else {
                syncStatusRepository.clearProgress()
                syncNotifier.notifySyncFailed(message)
                Result.failure()
            }
        }
    }

    private suspend fun refreshOidcSession(): kotlin.Result<Unit> {
        val credentials = settingsRepository.credentials.value
        if (
            credentials.authMethod != MealieAuthMethod.Oidc ||
                !oidcAuthClient.isExpiringSoon(credentials.apiToken)
        ) {
            return kotlin.Result.success(Unit)
        }
        val refreshed = oidcAuthClient.refresh(credentials)
        refreshed.onSuccess { settingsRepository.saveOidc(credentials.serverUrl, it) }
        return if (refreshed.isSuccess) kotlin.Result.success(Unit)
        else kotlin.Result.failure(refreshed.exceptionOrNull()!!)
    }

    private suspend fun syncStep(
        message: String,
        operation: suspend () -> kotlin.Result<Unit>,
    ): kotlin.Result<Unit> {
        syncStatusRepository.publishProgress(message)
        return operation()
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
