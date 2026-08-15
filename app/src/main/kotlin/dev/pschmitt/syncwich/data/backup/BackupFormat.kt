package dev.pschmitt.syncwich.data.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.annotation.Keep
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pschmitt.syncwich.BuildConfig
import dev.pschmitt.syncwich.data.db.AppDatabase
import dev.pschmitt.syncwich.data.settings.SettingsBackupSnapshot
import dev.pschmitt.syncwich.data.settings.SettingsRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BACKUP_FORMAT = "dev.pschmitt.syncwich.backup"
const val BACKUP_FORMAT_VERSION = 1

@Keep
@Serializable
data class BackupManifest(
    val format: String = BACKUP_FORMAT,
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val applicationId: String = BuildConfig.APPLICATION_ID,
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val createdAt: Long,
    val roomSchemaVersion: Int = AppDatabase.SCHEMA_VERSION,
    val includesCache: Boolean = false,
    val imageCount: Int = 0,
)

@Keep
@Serializable
data class BackupCredentials(
    val serverUrl: String = "",
    val apiToken: String = "",
)

data class BackupPayload(
    val manifest: BackupManifest,
    val credentials: BackupCredentials,
    val settings: SettingsBackupSnapshot,
    val databaseFile: File?,
    val imageDirectory: File?,
)

class BackupFormatException(message: String) : Exception(message)

class BackupPasswordRequiredException : Exception("This backup is password-protected")

class BackupWrongPasswordException : Exception("Incorrect backup password")

/** Password protection for the complete archive, using authenticated AES-GCM. */
object BackupCrypto {
    private val magic =
        byteArrayOf('S'.code.toByte(), 'W'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
    private val zipMagic =
        byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3.toByte(), 4.toByte())
    private const val plainFlag: Byte = 0
    private const val encryptedFlag: Byte = 1
    private const val saltSize = 16
    private const val ivSize = 12
    private const val tagBits = 128
    private const val iterations = 210_000
    private const val keyBits = 256

    fun encode(data: ByteArray, password: String?): ByteArray {
        if (password.isNullOrEmpty()) return magic + byteArrayOf(plainFlag) + data
        val salt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        val iv = ByteArray(ivSize).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(tagBits, iv))
        return magic + byteArrayOf(encryptedFlag) + salt + iv + cipher.doFinal(data)
    }

    fun decode(bytes: ByteArray, password: String?): ByteArray {
        if (bytes.size < magic.size + 1 || !bytes.copyOfRange(0, magic.size).contentEquals(magic)) {
            // Early development builds wrote the ZIP archive directly before the SWB1 envelope
            // was introduced. Keep those unencrypted backups restorable, but never treat an
            // unknown file as a password-protected backup.
            if (isRawZip(bytes) && password.isNullOrEmpty()) return bytes
            throw BackupFormatException("Not a valid Syncwich backup")
        }
        val payload = bytes.copyOfRange(magic.size + 1, bytes.size)
        return when (bytes[magic.size]) {
            plainFlag -> payload
            encryptedFlag -> {
                if (password.isNullOrEmpty()) throw BackupPasswordRequiredException()
                if (payload.size < saltSize + ivSize) {
                    throw BackupFormatException("The backup file is incomplete")
                }
                val salt = payload.copyOfRange(0, saltSize)
                val iv = payload.copyOfRange(saltSize, saltSize + ivSize)
                val ciphertext = payload.copyOfRange(saltSize + ivSize, payload.size)
                try {
                    Cipher.getInstance("AES/GCM/NoPadding").run {
                        init(
                            Cipher.DECRYPT_MODE,
                            deriveKey(password, salt),
                            GCMParameterSpec(tagBits, iv),
                        )
                        doFinal(ciphertext)
                    }
                } catch (_: Exception) {
                    throw BackupWrongPasswordException()
                }
            }
            else -> throw BackupFormatException("The backup uses an unknown encryption mode")
        }
    }

    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < magic.size + 1 || !bytes.copyOfRange(0, magic.size).contentEquals(magic)) {
            if (isRawZip(bytes)) return false
            throw BackupFormatException("Not a valid Syncwich backup")
        }
        return bytes[magic.size] == encryptedFlag
    }

    private fun isRawZip(bytes: ByteArray): Boolean =
        bytes.size >= zipMagic.size &&
            bytes.copyOfRange(0, zipMagic.size).contentEquals(zipMagic)

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyBits)
        return SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES",
        )
    }
}

@Singleton
class BackupManager
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    suspend fun write(uri: Uri, password: String?) {
        withContext(Dispatchers.IO) {
            val archive = createArchive()
            val encoded = BackupCrypto.encode(archive, password?.takeIf(String::isNotEmpty))
            context.contentResolver.openOutputStream(uri)?.use { it.write(encoded) }
                ?: throw IOException("Could not open the selected file for writing")
        }
    }

    suspend fun restore(uri: Uri, password: String?): BackupManifest =
        withContext(Dispatchers.IO) {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().also { require(it.size <= MAX_ARCHIVE_BYTES) }
                } ?: throw IOException("Could not open the selected backup")
            val payload = decodeArchive(BackupCrypto.decode(bytes, password))
            validate(payload.manifest)
            if (payload.credentials.serverUrl.isBlank() || payload.credentials.apiToken.isBlank()) {
                throw BackupFormatException("The backup has no usable Mealie connection")
            }
            restoreCache(payload)
            settingsRepository.restoreBackupSettings(payload.settings)
            settingsRepository.save(payload.credentials.serverUrl, payload.credentials.apiToken)
            payload.manifest
        }

    suspend fun isEncrypted(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Could not open the selected backup")
            BackupCrypto.isEncrypted(bytes)
        }

    private suspend fun createArchive(): ByteArray {
        checkpointDatabase()
        val settings = settingsRepository.exportBackupSettings()
        val credentials = settingsRepository.credentials.value
        val dbFile = context.getDatabasePath(DATABASE_NAME).takeIf(File::exists)
        val imageRoot = context.cacheDir.resolve(IMAGE_CACHE_DIRECTORY)
        val imageFiles =
            imageRoot.walkTopDown().filter { it.isFile && it.length() <= MAX_ENTRY_BYTES }.toList()
        val manifest =
            BackupManifest(
                createdAt = System.currentTimeMillis(),
                roomSchemaVersion = AppDatabase.SCHEMA_VERSION,
                includesCache = dbFile != null,
                imageCount = imageFiles.size,
            )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            writeEntry(zip, MANIFEST_ENTRY, json.encodeToString(manifest).toByteArray())
            writeEntry(
                zip,
                CREDENTIALS_ENTRY,
                json
                    .encodeToString(BackupCredentials(credentials.serverUrl, credentials.apiToken))
                    .toByteArray(),
            )
            writeEntry(zip, SETTINGS_ENTRY, json.encodeToString(settings).toByteArray())
            dbFile?.let { writeFileEntry(zip, DATABASE_ENTRY, it) }
            imageFiles.forEach { file ->
                val relative = file.relativeTo(imageRoot).invariantSeparatorsPath
                writeFileEntry(zip, "$IMAGES_PREFIX/$relative", file)
            }
        }
        return output.toByteArray()
    }

    private fun decodeArchive(bytes: ByteArray): BackupPayload {
        val temporaryDirectory =
            File.createTempFile("syncwich-backup-", "", context.cacheDir).apply {
                delete()
                mkdirs()
            }
        var manifest: BackupManifest? = null
        var credentials = BackupCredentials()
        var settings = SettingsBackupSnapshot()
        var databaseFile: File? = null
        var imageDirectory: File? = null
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith('/')) {
                        throw BackupFormatException("The backup contains an unsafe file path")
                    }
                    val entryBytes = zip.readEntryBytes()
                    when (entryName) {
                        MANIFEST_ENTRY ->
                            manifest =
                                json.decodeFromString<BackupManifest>(entryBytes.decodeToString())
                        CREDENTIALS_ENTRY ->
                            credentials =
                                json.decodeFromString<BackupCredentials>(
                                    entryBytes.decodeToString()
                                )
                        SETTINGS_ENTRY ->
                            settings =
                                json.decodeFromString<SettingsBackupSnapshot>(
                                    entryBytes.decodeToString()
                                )
                        DATABASE_ENTRY -> {
                            databaseFile = temporaryDirectory.resolve(DATABASE_NAME)
                            databaseFile!!.writeBytes(entryBytes)
                        }
                        else ->
                            if (entryName.startsWith("$IMAGES_PREFIX/")) {
                                imageDirectory =
                                    imageDirectory
                                        ?: temporaryDirectory
                                            .resolve(IMAGES_PREFIX)
                                            .also(File::mkdirs)
                                val relative = entryName.removePrefix("$IMAGES_PREFIX/")
                                val output = imageDirectory!!.resolve(relative)
                                output.parentFile?.mkdirs()
                                output.writeBytes(entryBytes)
                            }
                    }
                    zip.closeEntry()
                }
            }
            return BackupPayload(
                manifest = manifest ?: throw BackupFormatException("The backup has no manifest"),
                credentials = credentials,
                settings = settings,
                databaseFile = databaseFile,
                imageDirectory = imageDirectory,
            )
        } catch (error: BackupFormatException) {
            temporaryDirectory.deleteRecursively()
            throw error
        } catch (error: Exception) {
            temporaryDirectory.deleteRecursively()
            throw BackupFormatException("Could not read the backup: ${error.message.orEmpty()}")
        }
    }

    private fun validate(manifest: BackupManifest) {
        if (manifest.format != BACKUP_FORMAT) throw BackupFormatException("Not a Syncwich backup")
        if (manifest.formatVersion > BACKUP_FORMAT_VERSION) {
            throw BackupFormatException("This backup was created by a newer Syncwich version")
        }
        if (!isCompatibleApplicationId(manifest.applicationId, BuildConfig.APPLICATION_ID)) {
            throw BackupFormatException("This backup belongs to a different application")
        }
        if (manifest.includesCache && manifest.roomSchemaVersion != AppDatabase.SCHEMA_VERSION) {
            throw BackupFormatException("This backup uses an incompatible cache schema")
        }
    }

    private suspend fun restoreCache(payload: BackupPayload) {
        payload.databaseFile?.let { restoreDatabase(it) }
        payload.imageDirectory?.let { restoreImages(it) }
        payload.databaseFile?.parentFile?.deleteRecursively()
        payload.imageDirectory?.parentFile?.takeIf(File::exists)?.deleteRecursively()
    }

    private fun checkpointDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    private fun restoreDatabase(sourceFile: File) {
        val source =
            SQLiteDatabase.openDatabase(sourceFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            if (source.version != AppDatabase.SCHEMA_VERSION) {
                throw BackupFormatException("This backup uses an incompatible cache schema")
            }
            val target = database.openHelper.writableDatabase
            val tables = applicationTables(source)
            target.execSQL("PRAGMA foreign_keys=OFF")
            target.beginTransaction()
            try {
                APPLICATION_TABLES.forEach { table -> target.execSQL("DELETE FROM `$table`") }
                tables.forEach { table -> copyTable(source, target, table) }
                target.setTransactionSuccessful()
            } finally {
                target.endTransaction()
                target.execSQL("PRAGMA foreign_keys=ON")
            }
        } finally {
            source.close()
        }
    }

    private fun applicationTables(source: SQLiteDatabase): List<String> {
        val available = mutableSetOf<String>()
        source
            .rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                null,
            )
            .use { cursor -> while (cursor.moveToNext()) available += cursor.getString(0) }
        if (!available.containsAll(APPLICATION_TABLES)) {
            throw BackupFormatException("The backup cache is incomplete")
        }
        return APPLICATION_TABLES.filter(available::contains)
    }

    private fun copyTable(source: SQLiteDatabase, target: SupportSQLiteDatabase, table: String) {
        source.rawQuery("SELECT * FROM `$table`", null).use { cursor ->
            val columns = cursor.columnNames
            while (cursor.moveToNext()) {
                val values = android.content.ContentValues(columns.size)
                columns.forEachIndexed { index, column ->
                    when (cursor.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> values.putNull(column)
                        Cursor.FIELD_TYPE_INTEGER -> values.put(column, cursor.getLong(index))
                        Cursor.FIELD_TYPE_FLOAT -> values.put(column, cursor.getDouble(index))
                        Cursor.FIELD_TYPE_BLOB -> values.put(column, cursor.getBlob(index))
                        else -> values.put(column, cursor.getString(index))
                    }
                }
                target.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }
    }

    private fun restoreImages(source: File) {
        val target = context.cacheDir.resolve(IMAGE_CACHE_DIRECTORY)
        source.walkTopDown().filter(File::isFile).forEach { file ->
            val output = target.resolve(file.relativeTo(source).invariantSeparatorsPath)
            output.parentFile?.mkdirs()
            file.copyTo(output, overwrite = true)
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeFileEntry(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun ZipInputStream.readEntryBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        copyTo(output, MAX_ENTRY_BYTES + 1)
        val bytes = output.toByteArray()
        if (bytes.size > MAX_ENTRY_BYTES)
            throw BackupFormatException("The backup contains an oversized file")
        return bytes
    }

    private companion object {
        const val DATABASE_NAME = "syncwich.db"
        const val DATABASE_ENTRY = "cache/$DATABASE_NAME"
        const val MANIFEST_ENTRY = "manifest.json"
        const val CREDENTIALS_ENTRY = "credentials.json"
        const val SETTINGS_ENTRY = "settings.json"
        const val IMAGES_PREFIX = "images"
        const val IMAGE_CACHE_DIRECTORY = "recipe_images"
        const val MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L
        const val MAX_ENTRY_BYTES = 256L * 1024L * 1024L
        val APPLICATION_TABLES =
            listOf(
                "recipe_summaries",
                "recipe_details",
                "recipe_actions",
                "recipe_timeline_events",
                "recipe_step_progress",
                "categories",
                "tags",
                "cookbooks",
                "recipe_category_cross_refs",
                "recipe_tag_cross_refs",
                "shopping_lists",
                "shopping_list_items",
                "meal_plan_entries",
                "recipe_cookbook_cross_refs",
            )
    }
}

internal fun isCompatibleApplicationId(
    backupApplicationId: String,
    currentApplicationId: String,
): Boolean =
    backupApplicationId.removeSuffix(".debug") == currentApplicationId.removeSuffix(".debug")

private fun ZipInputStream.copyTo(output: ByteArrayOutputStream, limit: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > limit) throw BackupFormatException("The backup contains an oversized file")
        output.write(buffer, 0, count)
    }
}
