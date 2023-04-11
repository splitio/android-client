package io.split.android.client.storage.cipher.provider;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import io.split.android.client.utils.logger.Logger;

@RequiresApi(api = Build.VERSION_CODES.M)
public class SecureKeyStorageProvider implements KeyProvider {

    private static final String KEY_ALIAS_PREFIX = "split_key_";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    public static final String ALGORITHM = "AES";
    public static final int KEY_SIZE = 256;

    private final String mAlias;

    public SecureKeyStorageProvider(String apiKey) {
        mAlias = KEY_ALIAS_PREFIX + apiKey;
    }


    @Nullable
    @Override
    public SecretKey getKey() {
        try {
            return getAESKeyWithAndroidKeystore();
        } catch (Exception e) {
            Logger.e("Error while getting key from Android KeyStore: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private SecretKey getAESKeyWithAndroidKeystore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (!keyStore.containsAlias(mAlias)) {
            return generateSecretKey(mAlias);
        }

        return (SecretKey) keyStore.getKey(mAlias, null);
    }

    private SecretKey generateSecretKey(String alias) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER);
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
        KeyGenParameterSpec keySpec = builder
                .setKeySize(KEY_SIZE)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setRandomizedEncryptionRequired(false)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build();
        keyGenerator.init(keySpec);

        // Key is generated and stored in the Android KeyStore
        return keyGenerator.generateKey();
    }
}
