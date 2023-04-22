package io.split.android.client.storage.cipher

import org.junit.Test
import kotlin.test.assertTrue

class SplitCipherFactoryTest {

    @Test
    fun encryptionEnabledReturnsCBCCipher() {
        SplitCipherFactory.create("abcdefghijklmnopqrstuvwxyz", true).run {
            assertTrue(this is CBCCipher)
        }
    }

    @Test
    fun encryptionDisabledReturnsNoOpCipher() {
        SplitCipherFactory.create("abcdefghijklmnopqrstuvwxyz", false).run {
            assertTrue(this is NoOpCipher)
        }
    }
}
