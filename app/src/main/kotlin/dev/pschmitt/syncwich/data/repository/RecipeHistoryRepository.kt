package dev.pschmitt.syncwich.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recipeHistoryDataStore by preferencesDataStore(name = "recipe_history")

/**
 * Durable local history of recipe detail pages opened in Syncwich. Only recipe IDs are stored; Home
 * resolves them against the Room summary cache so an evicted or deleted recipe simply does not
 * appear until it is cached again.
 */
@Singleton
class RecipeHistoryRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    /** Most-recently-opened recipe ID first, including IDs whose Room summary is not cached yet. */
    val recipeIds: Flow<List<String>> =
        context.recipeHistoryDataStore.data
            .map { preferences -> decodeRecipeHistory(json, preferences[RECIPE_HISTORY]) }
            .distinctUntilChanged()

    /** Moves [recipeId] to the front, removes duplicates, and keeps the history bounded. */
    suspend fun recordOpen(recipeId: String) {
        context.recipeHistoryDataStore.edit { preferences ->
            val current = decodeRecipeHistory(json, preferences[RECIPE_HISTORY])
            preferences[RECIPE_HISTORY] =
                json.encodeToString(updatedRecipeHistory(current, recipeId))
        }
    }

    suspend fun clear() {
        context.recipeHistoryDataStore.edit { preferences -> preferences.remove(RECIPE_HISTORY) }
    }

    private companion object {
        val RECIPE_HISTORY = stringPreferencesKey("recipe_ids")
    }
}

internal const val MAX_RECIPE_HISTORY_ENTRIES = 20

/** Pure history update logic, kept separate for deterministic ordering and duplicate coverage. */
internal fun updatedRecipeHistory(
    current: List<String>,
    recipeId: String,
    limit: Int = MAX_RECIPE_HISTORY_ENTRIES,
): List<String> {
    val normalizedId = recipeId.trim()
    val normalizedCurrent = current.map(String::trim).filter(String::isNotEmpty)
    if (normalizedId.isEmpty()) return normalizedCurrent.distinct().take(limit.coerceAtLeast(0))

    return buildList {
            add(normalizedId)
            addAll(normalizedCurrent.filterNot { it == normalizedId })
        }
        .distinct()
        .take(limit.coerceAtLeast(0))
}

private fun decodeRecipeHistory(json: Json, serialized: String?): List<String> =
    serialized
        ?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
        }
        .orEmpty()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
