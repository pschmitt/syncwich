package dev.pschmitt.syncwich.data.settings

/** Frequencies supported by the optional local backup worker. */
enum class BackupFrequency(val storageValue: String, val intervalDays: Long, val label: String) {
    Daily("daily", 1L, "Daily"),
    Weekly("weekly", 7L, "Weekly"),
    Monthly("monthly", 30L, "Monthly"),
    ;

    companion object {
        fun fromStorage(value: String?): BackupFrequency =
            entries.firstOrNull { it.storageValue == value } ?: Weekly
    }
}
