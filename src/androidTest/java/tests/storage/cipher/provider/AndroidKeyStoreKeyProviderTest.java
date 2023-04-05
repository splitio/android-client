package tests.storage.cipher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.provider.AndroidKeyStoreKeyProvider;

public class AndroidKeyStoreKeyProviderTest {

    private AndroidKeyStoreKeyProvider mProvider;

    @Before
    public void setUp() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        mProvider = new AndroidKeyStoreKeyProvider();
    }

    @Test
    public void getKeyReturnsNonNullKey() {
        SecretKey key = mProvider.getKey("alias");

        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
    }

    @Test
    public void getKeyWhenKeyExistsDoesNotCreateNewOne() {
        SecretKey key = mProvider.getKey("alias");
        SecretKey key2 = mProvider.getKey("alias");

        assertNotNull(key);
        assertEquals(key, key2);
    }
}
