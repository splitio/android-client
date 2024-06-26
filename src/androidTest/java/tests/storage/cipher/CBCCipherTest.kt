package tests.storage.cipher

import helper.MOCK_DATA_EVENT
import helper.MOCK_DATA_IMPRESSION
import helper.MOCK_DATA_LONG_TEXT
import helper.MOCK_DATA_SPLIT
import io.split.android.client.storage.cipher.CBCCipher
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.DefaultAsserter.assertNotEquals

class CBCCipherTest {

    private lateinit var cipher: CBCCipher

    @Before
    fun setUp() {
        cipher = CBCCipher("abcdefghijklmnopqrstuvwxyz")
    }

    @Test
    fun testSplitsEncryption() {
        encryptDecryptTest(MOCK_DATA_SPLIT)
    }

    @Test
    fun testImpressionsEncryption() {
        encryptDecryptTest(MOCK_DATA_IMPRESSION)
    }

    @Test
    fun testEventsEncryption() {
        encryptDecryptTest(MOCK_DATA_EVENT)
    }

    @Test
    fun testLongTextEncryption() {
        StringBuilder().apply {
            for (i in 0..1000) {
                append(MOCK_DATA_LONG_TEXT)
            }
        }.run {
            encryptDecryptTest(toString())
        }
    }

    @Test
    fun testEncryptWithOneInstanceAndDecryptWithOther() {
        val testData = cipher.encrypt(MOCK_DATA_SPLIT)
        val secondCipher = CBCCipher("abcdefghijklmnopqrstuvwxyz")
        val decryptedData = secondCipher.decrypt(testData)
        assertEquals(MOCK_DATA_SPLIT, decryptedData)
    }

    private fun encryptDecryptTest(plainText: String) {
        val encrypted = cipher.encrypt(plainText)
        val decrypted = cipher.decrypt(encrypted)

        assertEquals(plainText, decrypted)
        assertNotEquals("Encrypted is equal to plainText", plainText, encrypted)
    }
}
