package dev.pschmitt.syncwich.ui.organizers

/**
 * Shared shape for [SimpleCatalogScreen]/[SimpleCatalogEditorScreen] - reused by Categories, Tags,
 * and Tools (SW-139), which are all `{id, name, slug}` Mealie organizers with an identical
 * name-only create/edit form. Units/Labels/Recipe Actions have meaningfully different fields and
 * get their own dedicated screens instead.
 */
data class SimpleCatalogItem(val id: String, val name: String)

sealed interface SimpleCatalogSaveState {
    data object Idle : SimpleCatalogSaveState

    data object Saving : SimpleCatalogSaveState

    data class Error(val message: String) : SimpleCatalogSaveState

    data object Saved : SimpleCatalogSaveState
}
