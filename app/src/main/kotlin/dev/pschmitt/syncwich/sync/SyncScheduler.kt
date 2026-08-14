package dev.pschmitt.syncwich.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.pschmitt.syncwich.data.repository.RecipeRepository
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import dev.pschmitt.syncwich.data.settings.SyncPreferences
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Enqueues [SyncWorker] periodically and once at app/onboarding startup. */
@Singleton
class SyncScheduler
@Inject
constructor(
    private val workManager: WorkManager,
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startupJob: Job? = null

    fun schedulePeriodic() {
        scope.launch { enqueuePeriodic(settingsRepository.syncPreferences.first()) }
    }

    private fun enqueuePeriodic(preferences: SyncPreferences) {
        val request =
            PeriodicWorkRequestBuilder<SyncWorker>(
                    preferences.syncIntervalHours.toLong(),
                    TimeUnit.HOURS,
                )
                .setConstraints(syncConstraints(preferences))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES,
                )
                .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Queues an immediate one-off sync without making the caller wait for it - safe to call from
     * both app startup (a no-op if not yet onboarded, see [SyncWorker.doWork]) and right after
     * onboarding succeeds (so the very first sync doesn't wait for the periodic schedule).
     */
    fun scheduleStartup() {
        startupJob?.cancel()
        startupJob = scope.launch { enqueueStartup(settingsRepository.syncPreferences.first()) }
    }

    /** Enqueues the automatic process-start pass only when the user has enabled it. */
    fun scheduleStartupIfEnabled() {
        startupJob?.cancel()
        startupJob = scope.launch {
            if (settingsRepository.syncOnAppStart.first()) {
                val hasCachedData =
                    recipeRepository.observeRecipes().first().isNotEmpty() ||
                        settingsRepository.initialSyncCompleted.first()
                delay(startupSyncDelayMillis(hasCachedData))
                enqueueStartup(settingsRepository.syncPreferences.first())
            } else {
                workManager.cancelUniqueWork(STARTUP_WORK_NAME)
            }
        }
    }

    private fun enqueueStartup(preferences: SyncPreferences) {
        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(syncConstraints(preferences))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES,
                )
                .build()
        workManager.enqueueUniqueWork(STARTUP_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Queues the non-blocking full pass used at startup for an explicit manual refresh. */
    fun syncAll() {
        scheduleStartup()
    }

    /** Removes a queued startup pass when the first sync is being run in the foreground. */
    fun cancelStartup() {
        startupJob?.cancel()
        startupJob = null
        workManager.cancelUniqueWork(STARTUP_WORK_NAME)
    }

    private fun syncConstraints(preferences: SyncPreferences): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                SyncNetworkPolicy.requiredNetworkType(
                    syncOnlyOnWifi = preferences.syncOnlyOnWifi,
                    syncWhileRoaming = preferences.syncWhileRoaming,
                )
            )
            .build()

    companion object {
        const val PERIODIC_WORK_NAME = "syncwich_periodic_sync"
        const val STARTUP_WORK_NAME = "syncwich_startup_sync"
        const val BACKOFF_DELAY_MINUTES = 15L
        const val STARTUP_SYNC_DELAY_MILLIS = 2_000L
    }
}

internal fun startupSyncDelayMillis(hasCachedData: Boolean): Long =
    if (hasCachedData) SyncScheduler.STARTUP_SYNC_DELAY_MILLIS else 0L
