package dev.pschmitt.syncwich.ui.foods

import dev.pschmitt.syncwich.data.api.dto.FoodMutationDto
import dev.pschmitt.syncwich.data.db.entity.FoodEntity

/** In-memory editor state kept intact when a save fails, including while the device is offline. */
data class FoodEditorDraft(
    val name: String = "",
    val pluralName: String = "",
    val description: String = "",
) {

    fun validationError(): String? =
        when {
            name.isBlank() -> "Enter a food name"
            name.trim().length > MAX_NAME_LENGTH ->
                "Food names must be $MAX_NAME_LENGTH characters or fewer"
            else -> null
        }

    fun toRequest(): FoodMutationDto =
        FoodMutationDto(
            name = name.trim(),
            pluralName = pluralName.trim().takeIf(String::isNotBlank),
            description = description.trim(),
        )

    companion object {
        private const val MAX_NAME_LENGTH = 200

        fun from(entity: FoodEntity): FoodEditorDraft =
            FoodEditorDraft(
                name = entity.name,
                pluralName = entity.pluralName.orEmpty(),
                description = entity.description,
            )

        fun seeded(name: String): FoodEditorDraft = FoodEditorDraft(name = name)
    }
}
