package dev.pschmitt.syncwich.data.backup

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncwichBackupFileName(now: Long = System.currentTimeMillis()): String =
    syncwichBackupFileName(deviceName = defaultDeviceName(), now = now)

internal fun syncwichBackupFileName(deviceName: String, now: Long): String {
    val sanitizedDeviceName =
        deviceName.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "android-device" }
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(now))
    return "Syncwich-$sanitizedDeviceName-$timestamp.syncwich"
}

private fun defaultDeviceName(): String = "${Build.MANUFACTURER}-${Build.MODEL}"
