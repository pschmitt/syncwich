package dev.pschmitt.syncwich.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One `/api/organizers/tags` item. */
@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey val id: String, val name: String, val slug: String)
