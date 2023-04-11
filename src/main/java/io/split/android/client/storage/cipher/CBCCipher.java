package io.split.android.client.storage.cipher;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class CBCCipher implements SplitCipher {

    private static final String SPEC = "AES/CBC/PKCS7Padding";
    private final SecretKey mKey;
    private final String mApiKey;
    private Cipher mEncryptCipher;
    private Cipher mDecryptCipher;

    @VisibleForTesting
    public CBCCipher(SecretKey key, @NonNull String apiKey) {
        mKey = key;
        mApiKey = apiKey;
    }

    public CBCCipher(String apiKey) {
        this(new KeyManager(apiKey).getKey(), apiKey);
    }

    @Override
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = getInitializedCipher(Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(data.getBytes(Charset.forName("UTF-8")));
        } catch (Exception e) {
            Logger.e("Error encrypting data: " + e.getMessage());
            return null;
        }

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    @Override
    public String decrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = getInitializedCipher(Cipher.DECRYPT_MODE);
        try {
            byte[] bytes = cipher.doFinal(Base64Util.bytesDecode(data));

            return new String(bytes, Charset.forName("UTF-8"));
        } catch (Exception e) {
            Logger.e("Error decrypting data: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private Cipher getInitializedCipher(int encryptMode) {
        if (encryptMode == Cipher.ENCRYPT_MODE && mEncryptCipher != null) {
            return mEncryptCipher;
        } else if (encryptMode == Cipher.DECRYPT_MODE && mDecryptCipher != null) {
            return mDecryptCipher;
        }

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(SPEC);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.e("Error getting cipher: " + e.getMessage());
            return null;
        }

        byte[] ivCBC = new byte[16];
        System.arraycopy(mApiKey.getBytes(), 0, ivCBC, 0, 16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivCBC);

        try {
            cipher.init(encryptMode, mKey, ivParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Logger.e("Error initializing cipher: " + e.getMessage());

            return null;
        }

        if (encryptMode == Cipher.ENCRYPT_MODE) {
            mEncryptCipher = cipher;
        } else {
            mDecryptCipher = cipher;
        }

        return cipher;
    }
}