package dev.pschmitt.syncwich.data.settings

import android.content.SharedPreferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.serialization.Serializable

/** A type-preserving, forward-compatible representation of one Android preference value. */
@Serializable
data class BackupPreferenceValue(
    val type: String,
    val value: String? = null,
    val values: List<String>? = null,
)

private const val TYPE_STRING = "string"
private const val TYPE_BOOLEAN = "boolean"
private const val TYPE_INT = "int"
private const val TYPE_LONG = "long"
private const val TYPE_FLOAT = "float"
private const val TYPE_DOUBLE = "double"
private const val TYPE_STRING_SET = "stringSet"

/** Converts every currently supported DataStore value without maintaining a second key list. */
fun Preferences.toBackupPreferences(): Map<String, BackupPreferenceValue> =
    asMap().map { (key, rawValue) -> key.name to rawValue.toBackupPreferenceValue() }.toMap()

/** Converts every currently supported SharedPreferences value without maintaining a key list. */
fun Map<String, *>.toBackupPreferences(): Map<String, BackupPreferenceValue> =
    mapValues { (_, rawValue) ->
        rawValue.toBackupPreferenceValue()
    }

private fun Any?.toBackupPreferenceValue(): BackupPreferenceValue =
    when (this) {
        is String -> BackupPreferenceValue(TYPE_STRING, value = this)
        is Boolean -> BackupPreferenceValue(TYPE_BOOLEAN, value = toString())
        is Int -> BackupPreferenceValue(TYPE_INT, value = toString())
        is Long -> BackupPreferenceValue(TYPE_LONG, value = toString())
        is Float -> BackupPreferenceValue(TYPE_FLOAT, value = toString())
        is Double -> BackupPreferenceValue(TYPE_DOUBLE, value = toString())
        is Set<*> -> {
            val strings = map { it as? String ?: error("Unsupported preference set value") }
            BackupPreferenceValue(TYPE_STRING_SET, values = strings.sorted())
        }
        else -> error("Unsupported preference value type: ${this?.javaClass?.name ?: "null"}")
    }

/** Restores all values into a DataStore transaction, including keys unknown to this app build. */
fun Map<String, BackupPreferenceValue>.restoreInto(preferences: MutablePreferences) {
    forEach { (key, value) -> value.writeTo(preferences, key) }
}

/** Restores all values into a SharedPreferences transaction. */
fun Map<String, BackupPreferenceValue>.restoreInto(editor: SharedPreferences.Editor) {
    forEach { (key, value) -> value.writeTo(editor, key) }
}

private fun BackupPreferenceValue.writeTo(preferences: MutablePreferences, key: String) {
    when (type) {
        TYPE_STRING -> preferences[stringPreferencesKey(key)] = value.orEmpty()
        TYPE_BOOLEAN -> preferences[booleanPreferencesKey(key)] = value.orEmpty().toBooleanStrict()
        TYPE_INT -> preferences[intPreferencesKey(key)] = value.orEmpty().toInt()
        TYPE_LONG -> preferences[longPreferencesKey(key)] = value.orEmpty().toLong()
        TYPE_FLOAT -> preferences[floatPreferencesKey(key)] = value.orEmpty().toFloat()
        TYPE_DOUBLE -> preferences[doublePreferencesKey(key)] = value.orEmpty().toDouble()
        TYPE_STRING_SET -> preferences[stringSetPreferencesKey(key)] = values.orEmpty().toSet()
        else -> error("Unsupported backup preference type: $type")
    }
}

private fun BackupPreferenceValue.writeTo(editor: SharedPreferences.Editor, key: String) {
    when (type) {
        TYPE_STRING -> editor.putString(key, value.orEmpty())
        TYPE_BOOLEAN -> editor.putBoolean(key, value.orEmpty().toBooleanStrict())
        TYPE_INT -> editor.putInt(key, value.orEmpty().toInt())
        TYPE_LONG -> editor.putLong(key, value.orEmpty().toLong())
        TYPE_FLOAT -> editor.putFloat(key, value.orEmpty().toFloat())
        TYPE_DOUBLE -> error("SharedPreferences cannot store Double values")
        TYPE_STRING_SET -> editor.putStringSet(key, values.orEmpty().toSet())
        else -> error("Unsupported backup preference type: $type")
    }
}
