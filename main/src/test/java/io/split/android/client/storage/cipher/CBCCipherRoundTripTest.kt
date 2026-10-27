package io.split.android.client.storage.cipher

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CBCCipherRoundTripTest {

    private val provider = JceCbcCipherProvider()

    @Test
    fun decryptReturnsOriginalValueAfterEncrypt() {
        val cipher = CBCCipher(provider)

        val encrypted = cipher.encrypt("some value")
        assertNotNull(encrypted)
        assertNotEquals("some value", encrypted)

        assertEquals("some value", cipher.decrypt(encrypted))
    }

    @Test
    fun decryptReturnsOriginalNonAsciiValueAfterEncrypt() {
        val cipher = CBCCipher(provider)
        val plain = "café ñ ✓"

        val encrypted = cipher.encrypt(plain)
        assertNotNull(encrypted)

        assertEquals(plain, cipher.decrypt(encrypted))
    }

    @Test
    fun decryptReturnsOriginalValueFromWrappedBase64() {
        val cipher = CBCCipher(provider)
        val plain = "Split feature flags keep rollouts safe while the SDK decrypts cached values after every restart."

        val encrypted = cipher.encrypt(plain)
        assertNotNull(encrypted)
        assertTrue(encrypted.length > 76, encrypted)
        val wrapped = encrypted.chunked(76).joinToString("\n")
        assertTrue(wrapped.contains("\n"), wrapped)

        assertEquals(plain, cipher.decrypt(wrapped))
    }

    @Test
    fun decryptReturnsOriginalValueFromBase64WithCrLfSpacesAndTabs() {
        val cipher = CBCCipher(provider)
        val plain = "Split feature flags keep rollouts safe while the SDK decrypts cached values after every restart."

        val encrypted = cipher.encrypt(plain)
        assertNotNull(encrypted)
        assertTrue(encrypted.length > 76, encrypted)
        val wrapped = encrypted.chunked(76).joinToString("\r\n") { " \t$it\t " }
        assertTrue(wrapped.contains("\r\n"), wrapped)
        assertTrue(wrapped.contains(" \t"), wrapped)

        assertEquals(plain, cipher.decrypt(wrapped))
    }
}
