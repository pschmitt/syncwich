package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/households/recipe-actions` item - see SW-139. Named distinctly from
 * [RecipeActionEntity] (this app's own local favorite/rating cache) to avoid confusion with
 * Mealie's unrelated concept. [groupId]/[householdId] are round-tripped on update, not editable.
 */
@Entity(tableName = "recipe_automations")
data class RecipeAutomationEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val title: String,
    val url: String,
    val groupId: String,
    val householdId: String,
)
