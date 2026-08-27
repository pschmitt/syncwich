package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/units` item - see SW-139. [rawJson] holds the full fetched `UnitDto`
 * (kotlinx.serialization JSON), so
 * [dev.pschmitt.syncwich.data.repository.UnitRepository.updateUnit] can round-trip fields this
 * app's editor doesn't expose (see [dev.pschmitt.syncwich.data.api.dto.UnitDto]'s kdoc), while
 * name/pluralName/description/abbreviation stay as columns for list display and search.
 */
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pluralName: String?,
    val description: String,
    val abbreviation: String,
    val rawJson: String,
)
