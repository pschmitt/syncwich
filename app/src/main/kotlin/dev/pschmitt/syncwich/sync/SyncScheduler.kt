package dev.pschmitt.syncwich.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues [SyncWorker] periodically and once at app/onboarding startup. */
@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

    fun schedulePeriodic() {
        val request =
            PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(syncConstraints())
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
        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(syncConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES,
                )
                .build()
        workManager.enqueueUniqueWork(STARTUP_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Removes a queued startup pass when the first sync is being run in the foreground. */
    fun cancelStartup() {
        workManager.cancelUniqueWork(STARTUP_WORK_NAME)
    }

    private fun syncConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val PERIODIC_WORK_NAME = "syncwich_periodic_sync"
        const val STARTUP_WORK_NAME = "syncwich_startup_sync"
        const val SYNC_INTERVAL_HOURS = 6L
        const val BACKOFF_DELAY_MINUTES = 15L
    }
}
