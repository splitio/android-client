package io.split.android.client.storage.cipher;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class CBCCipher implements SplitCipher {

    private static final String SPEC = "AES/CBC/PKCS7Padding";
    private static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
    private static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final IvParameterSpec mIvParameterSpec;
    private final SecretKey mKey;

    public CBCCipher(@NonNull String apiKey) {
        mKey = new KeyManager(apiKey).getKey();
        byte[] ivCBC = new byte[16];
        System.arraycopy(apiKey.getBytes(), 0, ivCBC, 0, 16);
        mIvParameterSpec = new IvParameterSpec(ivCBC);
    }

    @Override
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = getInitializedCipher(ENCRYPT_MODE);
        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(data.getBytes(CHARSET));
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

        Cipher cipher = getInitializedCipher(DECRYPT_MODE);
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
        try {
            Cipher cipher = Cipher.getInstance(SPEC);
            cipher.init(encryptMode, mKey, mIvParameterSpec);

            return cipher;
        } catch (InvalidAlgorithmParameterException | InvalidKeyException |
                 NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.e("Error initializing cipher: " + e.getMessage());

            return null;
        }
    }
}
