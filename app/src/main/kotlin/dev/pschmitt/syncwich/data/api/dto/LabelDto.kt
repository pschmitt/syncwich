package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * Mealie's multi-purpose label catalog item (`/api/groups/labels`), confirmed live via
 * `/openapi.json` (`MultiPurposeLabelOut`), e.g.
 * `{"name":"Pantry","color":"#959595","groupId":"...","id":"..."}` (SW-139).
 */
@Serializable
data class LabelDto(
    val id: String,
    val groupId: String,
    val name: String,
    val color: String = "#959595",
)

/** `POST /api/groups/labels` body (`MultiPurposeLabelCreate`). */
@Serializable data class LabelCreateDto(val name: String, val color: String = "#959595")

/**
 * `PUT /api/groups/labels/{id}` body (`MultiPurposeLabelUpdate`) - unlike Categories/Tags/Tools,
 * this requires [groupId] and [id] in the body itself, round-tripped from the cached [LabelDto]
 * (see `LabelRepository.updateLabel`).
 */
@Serializable
data class LabelUpdateDto(
    val name: String,
    val color: String = "#959595",
    val groupId: String,
    val id: String,
)
