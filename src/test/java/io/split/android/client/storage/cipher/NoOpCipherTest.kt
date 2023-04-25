package io.split.android.client.storage.cipher

import org.junit.Before
import org.junit.Test
import kotlin.test.DefaultAsserter.assertEquals

class NoOpCipherTest {

    private lateinit var cipher: NoOpCipher

    @Before
    fun setUp() {
        cipher = NoOpCipher()
    }

    @Test
    fun encryptionDoesNothing() {
        assertEquals(
            "Encrypted value is not equal to source",
            "test",
            cipher.encrypt("test")
        )
    }

    @Test
    fun decryptionDoesNothing() {
        assertEquals(
            "Decrypted value is not equal to source",
            "test",
            cipher.decrypt("test")
        )
    }
}
