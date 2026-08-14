package dev.pschmitt.syncwich.sync

import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** The user-facing state of a cache refresh. */
enum class SyncStatusState {
    NEVER_SYNCED,
    SYNCING,
    SYNCED,
    STALE,
    ERROR,
}

data class SyncStatus(
    val state: SyncStatusState = SyncStatusState.NEVER_SYNCED,
    val lastSyncAt: Long? = null,
    val errorMessage: String? = null,
    val currentMessage: String? = null,
)

/**
 * Combines WorkManager's persisted running state with the last completed sync metadata. Reading
 * this never performs network or Room work, so Home can show it alongside cached content without
 * making the content wait for a refresh.
 */
@Singleton
class SyncStatusRepository
@Inject
constructor(
    workManager: WorkManager,
    settingsRepository: SettingsRepository,
) {

    private val isSyncing: Flow<Boolean> =
        combine(
            workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.PERIODIC_WORK_NAME),
            workManager.getWorkInfosForUniqueWorkFlow(SyncScheduler.STARTUP_WORK_NAME),
        ) { periodic, startup ->
            (periodic + startup).any { it.state == WorkInfo.State.RUNNING }
        }

    private val currentMessage = MutableStateFlow<String?>(null)

    val status: Flow<SyncStatus> =
        combine(
            isSyncing,
            settingsRepository.lastSyncAt,
            settingsRepository.lastSyncError,
            currentMessage,
            statusClock,
        ) { syncing, lastSyncAt, errorMessage, progress, nowMillis ->
            resolveSyncStatus(
                isSyncing = syncing,
                lastSyncAt = lastSyncAt,
                errorMessage = errorMessage,
                nowMillis = nowMillis,
                currentMessage = progress,
            )
        }

    fun publishProgress(message: String) {
        currentMessage.value = message
    }

    fun clearProgress() {
        currentMessage.value = null
    }

    private companion object {
        const val STATUS_CLOCK_INTERVAL_MILLIS = 60_000L

        val statusClock: Flow<Long> =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(System.currentTimeMillis())
                    delay(STATUS_CLOCK_INTERVAL_MILLIS)
                }
            }
    }
}

internal fun resolveSyncStatus(
    isSyncing: Boolean,
    lastSyncAt: Long?,
    errorMessage: String?,
    nowMillis: Long,
    staleAfterMillis: Long = 12 * 60 * 60 * 1_000L,
    currentMessage: String? = null,
): SyncStatus {
    val state =
        when {
            isSyncing -> SyncStatusState.SYNCING
            !errorMessage.isNullOrBlank() -> SyncStatusState.ERROR
            lastSyncAt == null -> SyncStatusState.NEVER_SYNCED
            nowMillis - lastSyncAt >= staleAfterMillis -> SyncStatusState.STALE
            else -> SyncStatusState.SYNCED
        }
    return SyncStatus(
        state = state,
        lastSyncAt = lastSyncAt,
        errorMessage = errorMessage,
        currentMessage = currentMessage,
    )
}
