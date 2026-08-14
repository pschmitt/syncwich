package dev.pschmitt.syncwich.data.backup

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
}
