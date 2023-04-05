package tests.storage.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.KeyManager;
import io.split.android.client.storage.cipher.provider.AndroidKeyStoreKeyProvider;
import io.split.android.client.storage.cipher.provider.LegacyKeyProvider;

public class KeyManagerTest {

    private KeyManager mKeyManager;

    @Test
    public void getKeyWithLegacyProvider() {
        mKeyManager = new KeyManager(new LegacyKeyProvider(ApplicationProvider.getApplicationContext().getApplicationContext()));

        SecretKey key = mKeyManager.getKey("my_api_key");

        assertNotNull(key);
    }

    @Test
    public void getExistingKeyWithLegacyProvider() {
        mKeyManager = new KeyManager(new LegacyKeyProvider(ApplicationProvider.getApplicationContext().getApplicationContext()));

        SecretKey key = mKeyManager.getKey("my_api_key");

        SecretKey secondKey = mKeyManager.getKey("my_api_key");

        assertNotNull(key);
        assertEquals(key, secondKey);
    }

    @Test
    public void getKeyWithAndroidKeyStoreProvider() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        mKeyManager = new KeyManager(new AndroidKeyStoreKeyProvider());

        SecretKey key = mKeyManager.getKey("my_api_key");

        assertNotNull(key);
    }

    @Test
    public void getExistingKeyWithAndroidKeyStoreProvider() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }

        mKeyManager = new KeyManager(new AndroidKeyStoreKeyProvider());

        SecretKey key = mKeyManager.getKey("my_api_key");

        SecretKey secondKey = mKeyManager.getKey("my_api_key");

        assertNotNull(key);
        assertEquals(key, secondKey);
    }
}
