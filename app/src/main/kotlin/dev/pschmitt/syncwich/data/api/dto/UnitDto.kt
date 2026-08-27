package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class UnitAliasDto(val name: String)

/**
 * Mealie's ingredient-unit catalog item (`/api/units`), confirmed against a live `/openapi.json`
 * (`IngredientUnit-Output`). Every field is modeled, not just the ones this app's editor exposes
 * (name/pluralName/description/abbreviation) - `UnitRepository.updateUnit` round-trips the rest
 * (fraction/useAbbreviation/pluralAbbreviation/aliases/standardQuantity/standardUnit/extras)
 * unchanged from the cached fetch, since a `PUT` that omitted them would reset each to this DTO's
 * default rather than preserve the user's existing server-side value.
 */
@Serializable
data class UnitDto(
    val id: String,
    val name: String,
    val pluralName: String? = null,
    val description: String = "",
    val extras: JsonObject? = null,
    val fraction: Boolean = true,
    val abbreviation: String = "",
    val pluralAbbreviation: String? = "",
    val useAbbreviation: Boolean = false,
    val aliases: List<UnitAliasDto> = emptyList(),
    val standardQuantity: Double? = null,
    val standardUnit: String? = null,
)

/** Shared body shape for `POST /api/units` and `PUT /api/units/{id}` (both `CreateIngredientUnit`). */
@Serializable
data class UnitMutationDto(
    val name: String,
    val pluralName: String? = null,
    val description: String = "",
    val extras: JsonObject? = null,
    val fraction: Boolean = true,
    val abbreviation: String = "",
    val pluralAbbreviation: String? = "",
    val useAbbreviation: Boolean = false,
    val aliases: List<UnitAliasDto> = emptyList(),
    val standardQuantity: Double? = null,
    val standardUnit: String? = null,
)
