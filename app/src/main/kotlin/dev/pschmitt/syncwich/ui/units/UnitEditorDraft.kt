package dev.pschmitt.syncwich.ui.units

import dev.pschmitt.syncwich.data.db.entity.UnitEntity

/** In-memory editor state kept intact when a save fails, including while the device is offline. */
data class UnitEditorDraft(
    val name: String = "",
    val pluralName: String = "",
    val description: String = "",
    val abbreviation: String = "",
) {

    fun validationError(): String? =
        when {
            name.isBlank() -> "Enter a unit name"
            name.trim().length > MAX_NAME_LENGTH ->
                "Unit names must be $MAX_NAME_LENGTH characters or fewer"
            else -> null
        }

    companion object {
        private const val MAX_NAME_LENGTH = 200

        fun from(entity: UnitEntity): UnitEditorDraft =
            UnitEditorDraft(
                name = entity.name,
                pluralName = entity.pluralName.orEmpty(),
                description = entity.description,
                abbreviation = entity.abbreviation,
            )
    }
}
