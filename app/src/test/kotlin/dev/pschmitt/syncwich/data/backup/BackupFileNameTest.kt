package dev.pschmitt.syncwich.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFileNameTest {

    private val timestamp = 1735787045000L

    private fun formattedTimestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(timestamp))

    @Test
    fun `backup filename includes sanitized device name and timestamp`() {
        assertEquals(
            "Syncwich-Google-Pixel-5-${formattedTimestamp()}.syncwich",
            syncwichBackupFileName("Google Pixel 5", timestamp),
        )
    }

    @Test
    fun `blank device name gets a safe fallback`() {
        assertEquals(
            "Syncwich-android-device-${formattedTimestamp()}.syncwich",
            syncwichBackupFileName("!!!", timestamp),
        )
    }
}
