package io.split.android.client.storage.cipher.provider;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.split.android.client.utils.logger.Logger;

public class LegacyKeyProvider implements KeyProvider {

    private static final String SHARED_PREFERENCES_NAME = "prefs_io.split.android.client";
    private static final String ALGORITHM = "AES";
    private final SharedPreferences mSharedPreferences;
    private final Base64Tool mBase64Tool;
    private KeyGenerator mKeyGenerator;

    public LegacyKeyProvider(@NonNull Context context) {
        this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE), new Base64Tool(), null);
    }

    @VisibleForTesting
    public LegacyKeyProvider(@NonNull SharedPreferences sharedPreferences,
                             @NonNull Base64Tool base64Tool,
                             @Nullable KeyGenerator keyGenerator) {
        mSharedPreferences = checkNotNull(sharedPreferences);
        mBase64Tool = checkNotNull(base64Tool);
        mKeyGenerator = keyGenerator;
    }

    @Nullable
    @Override
    public SecretKey getKey(@NonNull String alias) {
        return getKeyFromSharedPreferences(alias);
    }

    @Nullable
    private SecretKey getKeyFromSharedPreferences(String alias) {
        String base64EncodedKey = mSharedPreferences.getString(alias, null);

        if (base64EncodedKey == null) {
            SecretKey secretKey = generateSharedPrefsKey();
            if (secretKey == null) {
                return null;
            }

            base64EncodedKey = mBase64Tool.encode(secretKey.getEncoded());

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(alias, base64EncodedKey);
            editor.apply();
        }

        byte[] keyBytes = mBase64Tool.decode(base64EncodedKey);

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Nullable
    private SecretKey generateSharedPrefsKey() {
        try {
            if (mKeyGenerator == null) {
                mKeyGenerator = KeyGenerator.getInstance(ALGORITHM);
                mKeyGenerator.init(256);
            }
            return mKeyGenerator.generateKey();
        } catch (Exception e) {
            Logger.e("Error generating key for shared preferences", e.getMessage());
            return null;
        }
    }

    public static class Base64Tool {
        public String encode(byte[] bytes) {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        public byte[] decode(String string) {
            return Base64.decode(string, Base64.DEFAULT);
        }
    }
}
