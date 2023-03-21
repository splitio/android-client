package tests.service

import helper.DataSample
import io.split.android.client.common.SplitCipherImpl
import io.split.android.client.service.ServiceConstants
import io.split.android.client.utils.logger.Logger
import org.junit.Assert
import org.junit.Test
import java.util.*

class CipherTest {
    private var dataSample = DataSample()
    private var cypher = SplitCipherImpl(ServiceConstants.SECRET_KEY);

    @Test
    public fun testBasicEncryptDecrypt() {
        encryptDecryptTest("split_abtest")
    }

    @Test
    public fun testShortKeyEncryptDecrypt() {
        cypher = SplitCipherImpl("0".repeat(10))
        encryptDecryptTest("split_abtest")
    }

    @Test
    public fun testLongKeyEncryptDecrypt() {
        cypher = SplitCipherImpl("0".repeat(10))
        encryptDecryptTest("split_abtest")
    }

    @Test
    public fun testJsonSplitEncryptDecrypt() {
        val text = dataSample.jsonSplit
        encryptDecryptTest( text)
    }
    @Test
    public fun testMySegmentsEncryptDecrypt() {
        val text = "segment1, segment2, segment_4, segment%5, segment-6, segment!7, segment#8, segment@9, segment_what"
        encryptDecryptTest( text)
    }
    @Test
    public fun testImpressionEncryptDecrypt() {
        val text = dataSample.jsonImpression
        encryptDecryptTest( text)
    }
    @Test
    public fun testEventEncryptDecrypt() {
        val text = dataSample.jsonEvent
        encryptDecryptTest( text)
    }
    @Test
    public fun testVeryLongTextEncryptDecrypt() {

        val text = dataSample.veryLongText
        encryptDecryptTest( text)
    }

    private fun encryptDecryptTest(plainText: String) {
        val encrypted = cypher.encrypt(plainText)
        val decrypted = cypher.decrypt(encrypted!!)

        Assert.assertEquals(plainText, decrypted)
        Assert.assertNotEquals(plainText, encrypted)
    }
}