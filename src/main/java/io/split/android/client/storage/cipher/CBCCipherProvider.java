package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.split.android.client.utils.logger.Logger;

public class CBCCipherProvider implements CipherProvider {

    private static final String SPEC = "AES/CBC/PKCS7Padding";

    private static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;

    private static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    public static final int MAX_POOL_SIZE = 4;
    public static final int IV_LENGTH = 16;

    private final ObjectPool<Cipher> mCipherPool;

    private final SecretKey mKey;
    private final IvParameterSpec mIvParameterSpec;

    public CBCCipherProvider(@NonNull String apiKey) {
        mKey = new KeyManager(apiKey).getKey();
        byte[] ivCBC = new byte[IV_LENGTH];
        System.arraycopy(apiKey.getBytes(), 0, ivCBC, 0, IV_LENGTH);
        mIvParameterSpec = new IvParameterSpec(ivCBC);
        mCipherPool = new ObjectPool<>(MAX_POOL_SIZE, new CipherFactory());
    }

    @Nullable
    @Override
    public Cipher getEncryptionCipher() {
        return getInitializedCipher(ENCRYPT_MODE);
    }

    @Nullable
    @Override
    public Cipher getDecryptionCipher() {
        return getInitializedCipher(DECRYPT_MODE);
    }

    @Override
    public void release(Cipher cipher) {
        mCipherPool.release(cipher);
    }

    @Nullable
    private Cipher getInitializedCipher(int encryptMode) {
        try {
            Cipher cipher = mCipherPool.acquire();
            cipher.init(encryptMode, mKey, mIvParameterSpec);

            return cipher;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Logger.e("Error initializing cipher: " + e.getMessage());

            return null;
        }
    }

    private static class CipherFactory implements ObjectPoolFactory<Cipher> {
        @Override
        public Cipher createObject() {
            try {
                return Cipher.getInstance(SPEC);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                Logger.e("Error creating cipher: " + e.getMessage());
                return null;
            }
        }
    }
}
