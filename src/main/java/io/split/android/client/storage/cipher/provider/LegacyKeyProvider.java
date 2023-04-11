package io.split.android.client.storage.cipher.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class LegacyKeyProvider implements KeyProvider {

    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 16;
    private final SecretKey mSecretKey;

    public LegacyKeyProvider(@NonNull String apiKey) {
        mSecretKey = new SecretKeySpec(sanitizeKey(apiKey).getBytes(), ALGORITHM);
    }

    @Nullable
    @Override
    public SecretKey getKey() {
        return mSecretKey;
    }

    private static String sanitizeKey(String key) {
        if (key.length() < KEY_LENGTH) {
            return keyFilled(key);
        } else if (key.length() > KEY_LENGTH) {
            return key.substring(0, KEY_LENGTH);
        }

        return key;
    }

    private static String keyFilled(String key) {
        int fill = KEY_LENGTH - key.length();
        StringBuilder str = new StringBuilder(fill);
        for (int i = 0; i < fill; i++) {
            str.append("0");
        }
        return key + str;
    }
}
