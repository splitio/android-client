package tests.service

import helper.DataSample
import io.split.android.client.common.SplitCipherImpl
import io.split.android.client.utils.logger.Logger
import org.junit.Assert
import org.junit.Test
import java.util.*

class CipherTest {
    private var dataSample = DataSample()
    private val cypher = SplitCipherImpl()
    private val key = "0".repeat(32)

    @Test
    public fun testBasicEncryptDecrypt() {
        encryptDecryptTest("split_abtest", key)
    }

    @Test
    public fun testShortKeyEncryptDecrypt() {
        encryptDecryptTest("split_abtest", "0".repeat(10))
    }

    @Test
    public fun testLongKeyEncryptDecrypt() {
        encryptDecryptTest("split_abtest", "0".repeat(100))
    }

    @Test
    public fun testJsonSplitEncryptDecrypt() {
        val text = dataSample.jsonSplit
        encryptDecryptTest( text, key)
    }
    @Test
    public fun testMySegmentsEncryptDecrypt() {
        val text = "segment1, segment2, segment_4, segment%5, segment-6, segment!7, segment#8, segment@9, segment_what"
        encryptDecryptTest( text, key)
    }
    @Test
    public fun testImpressionEncryptDecrypt() {
        val text = dataSample.jsonImpression
        encryptDecryptTest( text, key)
    }
    @Test
    public fun testEventEncryptDecrypt() {
        val text = dataSample.jsonEvent
        encryptDecryptTest( text, key)
    }
    @Test
    public fun testVeryLongTextEncryptDecrypt() {

        val text = dataSample.veryLongText
        encryptDecryptTest( text, key)
    }

    private fun encryptDecryptTest(plainText: String, key: String) {
        val encrypted = cypher.encrypt(plainText, key)
        val decrypted = cypher.decrypt(encrypted!!, key)

        Assert.assertEquals(plainText, decrypted)
        Assert.assertNotEquals(plainText, encrypted)
    }
}