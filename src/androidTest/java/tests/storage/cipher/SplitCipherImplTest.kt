package tests.storage.cipher

import helper.MOCK_DATA_EVENT
import helper.MOCK_DATA_IMPRESSION
import helper.MOCK_DATA_LONG_TEXT
import helper.MOCK_DATA_SPLIT
import io.split.android.client.storage.cipher.SplitCipherImpl
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.DefaultAsserter.assertNotEquals

class SplitCipherImplTest {

    private lateinit var cipher: SplitCipherImpl

    @Before
    fun setUp() {
        cipher = SplitCipherImpl("abcdefghijklmnopqrstuvwxyz")
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
        StringBuilder().append(MOCK_DATA_LONG_TEXT).apply {
            for (i in 0..1000) {
                append(MOCK_DATA_LONG_TEXT)
            }
        }.let {
            encryptDecryptTest(it.toString())
        }
    }

    @Test
    fun testEncryptWithOneInstanceAndDecryptWithOther() {
        val testData = cipher.encrypt(MOCK_DATA_SPLIT)
        val secondCipher = SplitCipherImpl("abcdefghijklmnopqrstuvwxyz")
        val decryptedData = secondCipher.decrypt(testData)
        assertEquals(MOCK_DATA_SPLIT, decryptedData)
    }

    private fun encryptDecryptTest(plainText: String) {
        val encrypted = cipher.encrypt(plainText)
        val decrypted = cipher.decrypt(encrypted!!)

        assertEquals(plainText, decrypted)
        assertNotEquals("Encrypted is equal to planText", plainText, encrypted)
    }
}
