package dev.pschmitt.syncwich.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pschmitt.syncwich.MainActivity
import dev.pschmitt.syncwich.R
import javax.inject.Inject
import javax.inject.Singleton

/** Posts low-priority sync results only when the app is not visible. */
@Singleton
@SuppressLint("MissingPermission")
class SyncNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    @Volatile private var appInForeground = false
    @Volatile private var syncActive = false
    @Volatile private var currentSyncText = "Refreshing saved data…"
    private var started = false

    /** Creates the channel without requesting notification permission. */
    fun start() {
        if (started) return
        started = true
        val channel =
            NotificationChannel(
                    CHANNEL_ID,
                    "Background sync",
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    description = "Reports when the cached recipe sync finishes or fails"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun onAppForeground() {
        appInForeground = true
        NotificationManagerCompat.from(context).cancel(SYNC_NOTIFICATION_ID)
    }

    fun onAppBackground() {
        appInForeground = false
        if (syncActive && notificationsAllowed()) postOngoingNotification()
    }

    fun notifySyncStarted() {
        syncActive = true
        currentSyncText = "Refreshing saved data…"
        if (!appInForeground && notificationsAllowed()) postOngoingNotification()
    }

    fun notifySyncRetry(attempt: Int) {
        syncActive = true
        currentSyncText = "Retrying sync (attempt $attempt)…"
        if (!appInForeground && notificationsAllowed()) postOngoingNotification()
    }

    fun notifySyncSucceeded() {
        syncActive = false
        NotificationManagerCompat.from(context).cancel(SYNC_NOTIFICATION_ID)
        if (!shouldPostBackgroundNotification(appInForeground, notificationsAllowed())) return
        NotificationManagerCompat.from(context)
            .notify(
                SYNC_NOTIFICATION_ID,
                notificationBuilder("Sync complete", "Your saved recipe data is up to date.")
                    .setAutoCancel(true)
                    .build(),
            )
    }

    fun notifySyncFailed(message: String) {
        syncActive = false
        NotificationManagerCompat.from(context).cancel(SYNC_NOTIFICATION_ID)
        if (!shouldPostBackgroundNotification(appInForeground, notificationsAllowed())) return
        NotificationManagerCompat.from(context)
            .notify(
                SYNC_NOTIFICATION_ID,
                notificationBuilder("Sync failed", "Showing cached data: $message")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .build(),
            )
    }

    private fun postOngoingNotification() {
        NotificationManagerCompat.from(context)
            .notify(
                SYNC_NOTIFICATION_ID,
                notificationBuilder("Syncing recipes", currentSyncText)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build(),
            )
    }

    private fun notificationBuilder(title: String, text: String): NotificationCompat.Builder {
        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "background_sync"
        private const val SYNC_NOTIFICATION_ID = 2001
    }
}

internal fun shouldPostBackgroundNotification(
    appInForeground: Boolean,
    notificationsAllowed: Boolean,
): Boolean = !appInForeground && notificationsAllowed
