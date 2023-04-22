package io.split.android.client.storage.cipher

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import javax.crypto.Cipher
import kotlin.test.assertNull

class CBCCipherTest {

    @Mock
    private lateinit var cipherProvider: CipherProvider

    @Mock
    private lateinit var cipherMock: Cipher

    private lateinit var cipher: CBCCipher

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cipher = CBCCipher(cipherProvider)
    }

    @Test
    fun cipherForEncryptionIsRetrievedFromProvider() {
        `when`(cipherProvider.encryptionCipher).thenReturn(cipherMock)
        `when`(cipherMock.doFinal(any())).thenReturn(byteArrayOf(1, 2, 3))

        cipher.encrypt("test")

        verify(cipherProvider).encryptionCipher
    }

    @Test
    fun cipherForDecryptionIsRetrievedFromProvider() {
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)

        // ignoring the exception since not relevant for this test
        runCatching { cipher.decrypt("test") }

        verify(cipherProvider).decryptionCipher
    }

    @Test
    fun nullDataIsReturnedWhenEncryptingNull() {
        `when`(cipherProvider.encryptionCipher).thenReturn(cipherMock)

        val result = cipher.encrypt(null)

        assertNull(result)
    }

    @Test
    fun nullDataIsReturnedWhenDecryptingNull() {
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)

        val result = cipher.decrypt(null)

        assertNull(result)
    }
}
