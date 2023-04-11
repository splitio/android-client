package tests.storage.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.storage.cipher.CBCCipher;

public class CBCCipherTest {

    private CBCCipher mCBCCipher;

    @Before
    public void setUp() {
        mCBCCipher = new CBCCipher("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testEncrypt() {
        String testData = mCBCCipher.encrypt("testData");

        System.out.println("Test data is encrypted as: " + testData);
        assertNotNull(testData);
    }

    @Test
    public void testDecrypt() {
        String testData = mCBCCipher.encrypt("testData");

        String decryptedData = mCBCCipher.decrypt(testData);

        assertEquals("testData", decryptedData);
    }

    @Test
    public void testWithDifferentInstances() {
        String testData = mCBCCipher.encrypt("testData");

        CBCCipher cbcCipher = new CBCCipher("abcdefghijklmnopqrstuvwxyz");
        String decryptedData = cbcCipher.decrypt(testData);

        assertEquals("testData", decryptedData);
    }
}
