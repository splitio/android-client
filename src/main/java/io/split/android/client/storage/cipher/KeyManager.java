package io.split.android.client.storage.cipher;

import androidx.annotation.VisibleForTesting;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.provider.AndroidKeyStoreKeyProvider;
import io.split.android.client.storage.cipher.provider.KeyProvider;
import io.split.android.client.storage.cipher.provider.LegacyKeyProvider;
import io.split.android.client.utils.logger.Logger;

public class KeyManager {


    private final KeyProvider mProvider;


    @VisibleForTesting
    public KeyManager(KeyProvider provider) {
        mProvider = provider;
    }

    public KeyManager(String apiKey) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Logger.d("Using AndroidKeyStoreKeyProvider");
            mProvider = new AndroidKeyStoreKeyProvider(apiKey);
        } else {
            Logger.d("Using LegacyKeyProvider");
            mProvider = new LegacyKeyProvider(apiKey);
        }
    }

    @Nullable
    public SecretKey getKey() {
        return mProvider.getKey();
    }
}
