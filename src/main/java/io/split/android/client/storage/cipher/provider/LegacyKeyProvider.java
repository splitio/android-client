package io.split.android.client.storage.cipher.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.split.android.client.utils.logger.Logger;

public class LegacyKeyProvider implements KeyProvider {

    private static final String SHARED_PREFERENCES_NAME = "split_prefs";
    private static final String ALGORITHM = "AES";
    private final SharedPreferences mSharedPreferences;
    private final Base64Wrapper mBase64Wrapper;
    private KeyGenerator mKeyGenerator;

    public LegacyKeyProvider(@NonNull Context context) {
        this(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE), new Base64Wrapper(), null);
    }

    public LegacyKeyProvider(@NonNull SharedPreferences sharedPreferences,
                             @NonNull Base64Wrapper base64Util,
                             @Nullable KeyGenerator keyGenerator) {
        mSharedPreferences = sharedPreferences;
        mBase64Wrapper = base64Util;
        mKeyGenerator = keyGenerator;
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

            base64EncodedKey = mBase64Wrapper.encode(secretKey.getEncoded());

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(alias, base64EncodedKey);
            editor.apply();
        }

        byte[] keyBytes = mBase64Wrapper.decode(base64EncodedKey);

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

    public static class Base64Wrapper {
        public String encode(byte[] bytes) {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        public byte[] decode(String string) {
            return Base64.decode(string, Base64.DEFAULT);
        }
    }
}
