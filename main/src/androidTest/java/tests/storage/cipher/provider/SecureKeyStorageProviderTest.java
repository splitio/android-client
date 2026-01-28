package tests.storage.cipher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.provider.SecureKeyStorageProvider;

public class SecureKeyStorageProviderTest {

    private SecureKeyStorageProvider mProvider;

    @Before
    public void setUp() {
        mProvider = new SecureKeyStorageProvider("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void getKeyReturnsNonNullKey() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        SecretKey key = mProvider.getKey();

        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
    }

    @Test
    public void getKeyWhenKeyExistsDoesNotCreateNewOne() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        SecretKey key = mProvider.getKey();
        SecretKey key2 = mProvider.getKey();

        assertNotNull(key);
        assertEquals(key, key2);
    }
}
