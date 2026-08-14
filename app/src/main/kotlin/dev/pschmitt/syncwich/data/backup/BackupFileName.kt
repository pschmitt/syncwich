package dev.pschmitt.syncwich.data.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun syncwichBackupFileName(now: Long = System.currentTimeMillis()): String =
    "Syncwich-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(now))}.syncwich"
