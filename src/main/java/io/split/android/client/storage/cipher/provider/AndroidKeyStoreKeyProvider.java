package io.split.android.client.storage.cipher.provider;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import io.split.android.client.utils.logger.Logger;

@RequiresApi(api = Build.VERSION_CODES.M)
public
class AndroidKeyStoreKeyProvider implements KeyProvider {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    @Override
    public SecretKey getKey(@NonNull String alias) {
        try {
            return getAESKeyWithAndroidKeystore(alias);
        } catch (Exception e) {
            Logger.e("Error while getting key from Android KeyStore: " + e.getMessage());
            return null;
        }
    }

    private SecretKey getAESKeyWithAndroidKeystore(String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        if (!keyStore.containsAlias(alias)) {
            SecretKey secretKey = generateSecretKey(alias);
            keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), null);
        }

        return (SecretKey) keyStore.getKey(alias, null);
    }

    private SecretKey generateSecretKey(String alias) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", KEYSTORE_PROVIDER);
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
        KeyGenParameterSpec keySpec = builder
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build();
        keyGenerator.init(keySpec);

        return keyGenerator.generateKey();
    }
}
