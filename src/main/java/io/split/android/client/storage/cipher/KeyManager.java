package io.split.android.client.storage.cipher;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import io.split.android.client.storage.cipher.provider.AndroidKeyStoreKeyProvider;
import io.split.android.client.storage.cipher.provider.KeyProvider;
import io.split.android.client.storage.cipher.provider.LegacyKeyProvider;
import io.split.android.client.utils.logger.Logger;

public class KeyManager {

    private static final String KEY_ALIAS_PREFIX = "split_key_";

    private final KeyProvider mProvider;

    @VisibleForTesting
    public KeyManager(KeyProvider provider) {
        mProvider = provider;
    }

    public KeyManager(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Logger.d("Using AndroidKeyStoreKeyProvider");
            mProvider = new AndroidKeyStoreKeyProvider();
        } else {
            Logger.d("Using LegacyKeyProvider");
            mProvider = new LegacyKeyProvider(context);
        }
    }

    @Nullable
    public SecretKey getAESKey(String apiKey) {
        return mProvider.getKey(KEY_ALIAS_PREFIX + apiKey);
    }
}
