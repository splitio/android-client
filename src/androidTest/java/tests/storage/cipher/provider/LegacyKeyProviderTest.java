package tests.storage.cipher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.provider.LegacyKeyProvider;

public class LegacyKeyProviderTest {

    private LegacyKeyProvider mProvider;

    @Before
    public void setUp() {
        mProvider = new LegacyKeyProvider("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void getKeyReturnsNonNullKey() {
        assertNotNull(mProvider.getKey());
    }

    @Test
    public void getKeyWhenKeyExistsDoesNotCreateNewOne() {
        SecretKey key = mProvider.getKey();
        SecretKey key2 = mProvider.getKey();
        assertNotNull(key);
        assertEquals(key, key2);
    }
}
