package io.split.android.client.storage.cipher

import io.split.android.client.utils.logger.Logger
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun failedDecryptLogsLengthAndClassButNotInput() {
        val input = "c2VjcmV0LXBheWxvYWQ="
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)
        `when`(cipherMock.doFinal(any())).thenThrow(BadPaddingException("bad padding"))

        val result: String?
        val logged: String
        Mockito.mockStatic(Logger::class.java).use { logger ->
            result = cipher.decrypt(input)
            val captor = ArgumentCaptor.forClass(String::class.java)
            logger.verify({ Logger.e(captor.capture()) })
            logged = captor.value
        }

        assertNull(result)
        assertEquals("Error decrypting data: input length 20, BadPaddingException", logged)
        assertFalse(logged.contains(input), logged)
        verify(cipherProvider).release(cipherMock)
    }

    @Test
    fun nullInputDecryptReturnsNullWithoutLogging() {
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)

        val result: String?
        Mockito.mockStatic(Logger::class.java).use { logger ->
            result = cipher.decrypt(null)
            logger.verifyNoInteractions()
        }

        assertNull(result)
    }

    @Test
    fun failedDecryptOfEmptyInputLogsZeroLengthAndClass() {
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)
        `when`(cipherMock.doFinal(any())).thenThrow(BadPaddingException("bad padding"))

        val result: String?
        val logged: String
        Mockito.mockStatic(Logger::class.java).use { logger ->
            result = cipher.decrypt("")
            val captor = ArgumentCaptor.forClass(String::class.java)
            logger.verify({ Logger.e(captor.capture()) })
            logged = captor.value
        }

        assertNull(result)
        assertEquals("Error decrypting data: input length 0, BadPaddingException", logged)
    }

    @Test
    fun undecodableInputLogsWithoutInputOrUnderlyingMessage() {
        val input = "not base64!!"
        `when`(cipherProvider.decryptionCipher).thenReturn(cipherMock)
        `when`(cipherMock.doFinal(any())).thenThrow(BadPaddingException("bad padding"))

        val result: String?
        val logged: List<String>
        Mockito.mockStatic(Logger::class.java).use { logger ->
            result = cipher.decrypt(input)
            val captor = ArgumentCaptor.forClass(String::class.java)
            logger.verify({ Logger.e(captor.capture()) }, Mockito.times(2))
            logged = captor.allValues
        }

        assertNull(result)
        assertEquals(2, logged.size)
        assertEquals(
            "Received bytes didn't correspond to a valid Base64 encoded string.Illegal base64 character 21",
            logged[0]
        )
        assertEquals("Error decrypting data: input length 12, BadPaddingException", logged[1])
        assertFalse(logged[1].contains(input), logged[1])
        assertFalse(logged[1].contains("Illegal base64 character"), logged[1])
    }
}
