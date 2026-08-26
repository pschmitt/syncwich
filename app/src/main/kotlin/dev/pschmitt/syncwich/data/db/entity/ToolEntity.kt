package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One `/api/organizers/tools` item - see SW-139. */
@Entity(tableName = "tools")
data class ToolEntity(@PrimaryKey val id: String, val name: String, val slug: String)
