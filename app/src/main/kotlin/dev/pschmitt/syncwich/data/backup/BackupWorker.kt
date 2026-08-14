package dev.pschmitt.syncwich.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class BackupWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val folder =
                settingsRepository.scheduledBackupFolderUri.first()?.let(Uri::parse)
                    ?: return@withContext Result.failure()
            try {
                val uri =
                    DocumentsContract.createDocument(
                        applicationContext.contentResolver,
                        folder,
                        "application/octet-stream",
                        syncwichBackupFileName(),
                    ) ?: error("Could not create a backup file in the selected folder")
                backupManager.write(uri, settingsRepository.scheduledBackupPassword())
                settingsRepository.recordBackupSuccess()
                Result.success()
            } catch (error: Exception) {
                Timber.e(error, "Scheduled Syncwich backup failed")
                settingsRepository.recordBackupFailure(
                    error.message?.takeIf(String::isNotBlank) ?: "Could not create a backup"
                )
                Result.retry()
            }
        }
}
