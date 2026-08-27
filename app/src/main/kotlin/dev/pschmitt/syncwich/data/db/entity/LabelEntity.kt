package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One `/api/groups/labels` item - see SW-139. [groupId] is round-tripped on update, not editable.
 */
@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val color: String,
)
