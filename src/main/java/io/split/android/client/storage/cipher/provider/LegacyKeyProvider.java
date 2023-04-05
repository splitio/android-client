package io.split.android.client.storage.cipher.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class LegacyKeyProvider implements KeyProvider {

    private static final String SHARED_PREFERENCES_NAME = "split_prefs";
    private static final String ALGORITHM = "AES";
    private final SharedPreferences mSharedPreferences;

    public LegacyKeyProvider(@NonNull Context context) {
        this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE));
    }

    public LegacyKeyProvider(@NonNull SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }

    @Nullable
    @Override
    public SecretKey getKey(@NonNull String alias) {
        return getKeyFromSharedPreferences(alias);
    }

    private SecretKey getKeyFromSharedPreferences(String alias) {
        String base64EncodedKey = mSharedPreferences.getString(alias, null);

        if (base64EncodedKey == null) {
            SecretKey secretKey = generateSharedPrefsKey();
            if (secretKey == null) {
                return null;
            }

            base64EncodedKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(alias, base64EncodedKey);
            editor.apply();
        }

        byte[] keyBytes = Base64.decode(base64EncodedKey, Base64.DEFAULT);

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Nullable
    private SecretKey generateSharedPrefsKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            return null;
        }
    }
}
