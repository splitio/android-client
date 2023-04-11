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
        mKeyManager = new KeyManager(new LegacyKeyProvider("abcdefghijklmnopqrstuvwxyz"));

        SecretKey key = mKeyManager.getKey();

        assertNotNull(key);
    }

    @Test
    public void getExistingKeyWithLegacyProvider() {
        mKeyManager = new KeyManager(new LegacyKeyProvider("abcdefghijklmnopqrstuvwxyz"));

        SecretKey key = mKeyManager.getKey();

        SecretKey secondKey = mKeyManager.getKey();

        assertNotNull(key);
        assertEquals(key, secondKey);
    }

    @Test
    public void getKeyWithAndroidKeyStoreProvider() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        mKeyManager = new KeyManager(new AndroidKeyStoreKeyProvider("abcdefghijklmnopqrstuvwxyz"));

        SecretKey key = mKeyManager.getKey();

        assertNotNull(key);
    }

    @Test
    public void getExistingKeyWithAndroidKeyStoreProvider() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }

        mKeyManager = new KeyManager(new AndroidKeyStoreKeyProvider("abcdefghijklmnopqrstuvwxyz"));

        SecretKey key = mKeyManager.getKey();

        SecretKey secondKey = mKeyManager.getKey();

        assertNotNull(key);
        assertEquals(key, secondKey);
    }
}
