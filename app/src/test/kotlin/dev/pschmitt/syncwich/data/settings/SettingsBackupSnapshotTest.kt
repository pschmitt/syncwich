package dev.pschmitt.syncwich.data.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBackupSnapshotTest {

    @Test
    fun `all user settings added to the snapshot survive JSON round trip`() {
        val snapshot =
            SettingsBackupSnapshot(
                settingsVersion = SETTINGS_BACKUP_VERSION,
                preferences =
                    mapOf(
                        "developer_mode" to BackupPreferenceValue(type = "boolean", value = "true"),
                        "font_scale" to BackupPreferenceValue(type = "float", value = "1.15"),
                    ),
                securePreferences =
                    mapOf(
                        "scheduled_backup_password" to
                            BackupPreferenceValue(type = "string", value = "scheduled-secret")
                    ),
                navigationBarVisibleItems = setOf(NavigationBarItemKeys.COOKBOOKS),
            )
        val json = Json { encodeDefaults = true }

        val restored = json.decodeFromString<SettingsBackupSnapshot>(json.encodeToString(snapshot))

        assertEquals(snapshot, restored)
    }

    @Test
    fun `backups without a settings version remain legacy imports`() {
        val json = Json { ignoreUnknownKeys = true }

        val restored =
            json.decodeFromString<SettingsBackupSnapshot>(
                "{\"developerMode\":true,\"scheduledBackupPassword\":\"old\"}"
            )

        assertEquals(1, restored.settingsVersion)
        assertEquals(true, restored.developerMode)
        assertEquals("old", restored.scheduledBackupPassword)
    }
}
