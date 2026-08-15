package dev.pschmitt.syncwich.data.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun unencryptedRoundTripIsSupported() {
        val input = "Syncwich backup".toByteArray()
        val encoded = BackupCrypto.encode(input, null)

        assertFalse(BackupCrypto.isEncrypted(encoded))
        assertArrayEquals(input, BackupCrypto.decode(encoded, null))
    }

    @Test
    fun encryptedRoundTripRequiresThePassword() {
        val input = "cached recipe data".toByteArray()
        val encoded = BackupCrypto.encode(input, "correct horse")

        assertTrue(BackupCrypto.isEncrypted(encoded))
        assertArrayEquals(input, BackupCrypto.decode(encoded, "correct horse"))
    }

    @Test(expected = BackupPasswordRequiredException::class)
    fun encryptedBackupWithoutPasswordIsRejected() {
        BackupCrypto.decode(BackupCrypto.encode(byteArrayOf(1, 2, 3), "secret"), null)
    }

    @Test(expected = BackupWrongPasswordException::class)
    fun encryptedBackupWithWrongPasswordIsRejected() {
        BackupCrypto.decode(BackupCrypto.encode(byteArrayOf(1, 2, 3), "secret"), "wrong")
    }

    @Test
    fun legacyUnencryptedZipIsAccepted() {
        val legacyArchive =
            ByteArrayOutputStream()
                .apply {
                    ZipOutputStream(this).use { zip ->
                        zip.putNextEntry(ZipEntry("manifest.json"))
                        zip.write("{}".toByteArray())
                        zip.closeEntry()
                    }
                }
                .toByteArray()

        assertFalse(BackupCrypto.isEncrypted(legacyArchive))
        assertArrayEquals(legacyArchive, BackupCrypto.decode(legacyArchive, null))
    }

    @Test(expected = BackupFormatException::class)
    fun unknownFileIsRejected() {
        BackupCrypto.decode("not a backup".toByteArray(), null)
    }
}
