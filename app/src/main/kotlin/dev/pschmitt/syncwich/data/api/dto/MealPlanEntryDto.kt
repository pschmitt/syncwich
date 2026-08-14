package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/households/mealplans` item shape (`ReadPlanEntry` in the server's OpenAPI schema),
 * confirmed against a live v3.22.0 instance's `/openapi.json` - the verification household had no
 * meal-plan entries at the time, so the paginated envelope/query params were confirmed against a
 * live (empty) response while individual item fields are pinned from the schema, not a populated
 * live payload; see `MealPlanApiDtoTest`. `entryType` is one of `breakfast`/`lunch`/`dinner`/`side`/
 * `snack`/`drink`/`dessert` per the schema's enum, kept as a raw string here rather than a
 * `@Serializable` enum so an unrecognized future value degrades to "unknown" in the UI instead of
 * failing to decode. `recipe` reuses [RecipeSummaryDto] - the server's embedded `RecipeSummary`
 * schema has the same fields.
 */
@Serializable
data class MealPlanEntryDto(
    val id: Int,
    val date: String,
    val entryType: String = "breakfast",
    val title: String = "",
    val text: String = "",
    val recipeId: String? = null,
    val recipe: RecipeSummaryDto? = null,
)
