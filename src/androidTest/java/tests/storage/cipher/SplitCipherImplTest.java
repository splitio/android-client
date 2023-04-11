package tests.storage.cipher;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.storage.cipher.SplitCipherImpl;

public class SplitCipherImplTest {

    private SplitCipherImpl mSplitCipherImpl;

    @Before
    public void setUp() {
        mSplitCipherImpl = new SplitCipherImpl("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testEncryptDecrypt() {
        String testData = mSplitCipherImpl.encrypt("testData");
        String decryptedData = mSplitCipherImpl.decrypt(testData);
        assertEquals("testData", decryptedData);
    }

    @Test
    public void testEncryptWithOneInstanceAndDecryptWithOther() {
        String testData = mSplitCipherImpl.encrypt("testData");
        SplitCipherImpl splitCipherImpl = new SplitCipherImpl("abcdefghijklmnopqrstuvwxyz");
        String decryptedData = splitCipherImpl.decrypt(testData);
        assertEquals("testData", decryptedData);
    }
}
