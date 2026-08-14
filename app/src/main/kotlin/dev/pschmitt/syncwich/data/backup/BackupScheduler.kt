package dev.pschmitt.syncwich.data.backup

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class BackupScheduler
@Inject
constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun schedule() {
        scope.launch {
            val enabled = settingsRepository.scheduledBackupEnabled.first()
            val folder = settingsRepository.scheduledBackupFolderUri.first()
            if (!enabled || folder.isNullOrBlank()) {
                cancel()
                return@launch
            }
            val frequency = settingsRepository.scheduledBackupFrequency.first()
            val request =
                PeriodicWorkRequestBuilder<BackupWorker>(
                        frequency.intervalDays,
                        TimeUnit.DAYS,
                    )
                    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                    .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "syncwich_scheduled_backup"
    }
}
