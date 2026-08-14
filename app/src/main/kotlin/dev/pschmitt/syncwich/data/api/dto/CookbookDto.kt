package dev.pschmitt.syncwich.data.api.dto

import kotlinx.serialization.Serializable

/**
 * `/api/households/cookbooks` item shape, confirmed against a live v3.22.0 Mealie instance - e.g.
 * `{"id":"...","name":"Chinese Nom Nom","slug":"chinese-nom-nom","description":"","position":1,
 * "public":false,"queryFilterString":"recipe_category.id IN [\"...\"]","groupId":"...",
 * "householdId":"...","queryFilter":{...},"household":{...}}`, wrapped in the same
 * `PagedResponseDto` envelope as the plain recipe and organizer lists. A cookbook is a saved
 * recipe-category/tag filter, not an embedded list of recipes - `queryFilter`/`household` aren't
 * modeled since the app's lenient `Json` config drops them and nothing here needs to re-evaluate the
 * filter client-side; its matching recipes are instead fetched live via `GET /api/recipes?cookbook={id}`
 * (also confirmed live), which returns the same
 * `PagedResponseDto<RecipeSummaryDto>` envelope as the plain recipe list - see
 * [dev.pschmitt.syncwich.data.api.RecipesApi.getRecipesByCookbook].
 */
@Serializable
data class CookbookDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val position: Int = 0,
    val public: Boolean = false,
    val queryFilterString: String? = null,
)
