package dev.pschmitt.syncwich.ui.cookbooks

import dev.pschmitt.syncwich.data.api.dto.CreateCookbookDto
import dev.pschmitt.syncwich.data.db.entity.CookbookEntity

/** In-memory editor state kept intact when a save fails, including while the device is offline. */
data class CookbookEditorDraft(
    val name: String = "",
    val description: String = "",
    val queryFilterString: String = "",
    val position: Int = 1,
    val public: Boolean = false,
    val existingSlug: String? = null,
) {

    fun validationError(): String? =
        when {
            name.isBlank() -> "Enter a cookbook name"
            name.trim().length > MAX_NAME_LENGTH ->
                "Cookbook names must be $MAX_NAME_LENGTH characters or fewer"
            else -> null
        }

    fun toRequest(): CreateCookbookDto =
        CreateCookbookDto(
            name = name.trim(),
            description = description.trim(),
            position = position,
            public = public,
            queryFilterString = queryFilterString.trim(),
            slug = existingSlug?.takeIf { it.isNotBlank() },
        )

    companion object {
        private const val MAX_NAME_LENGTH = 200

        fun from(entity: CookbookEntity): CookbookEditorDraft =
            CookbookEditorDraft(
                name = entity.name,
                description = entity.description,
                queryFilterString = entity.queryFilterString,
                position = entity.position,
                public = entity.public,
                existingSlug = entity.slug,
            )
    }
}
