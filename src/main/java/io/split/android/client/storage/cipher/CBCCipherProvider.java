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

    private Cipher mCipher;

    private final SecretKey mKey;
    private final IvParameterSpec mIvParameterSpec;

    public CBCCipherProvider(@NonNull String apiKey) {
        mKey = new KeyManager(apiKey).getKey();
        byte[] ivCBC = new byte[16];
        System.arraycopy(apiKey.getBytes(), 0, ivCBC, 0, 16);
        mIvParameterSpec = new IvParameterSpec(ivCBC);
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

    @Nullable
    private synchronized Cipher getInitializedCipher(int encryptMode) {
        try {
            if (mCipher == null) {
                mCipher = Cipher.getInstance(SPEC);
            }
            mCipher.init(encryptMode, mKey, mIvParameterSpec);

            return mCipher;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException |
                 NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.e("Error initializing cipher: " + e.getMessage());

            return null;
        }
    }
}
