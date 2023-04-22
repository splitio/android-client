package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.nio.charset.Charset;

import javax.crypto.Cipher;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class CBCCipher implements SplitCipher {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final CipherProvider mCipherProvider;

    public CBCCipher(@NonNull String apiKey) {
        this(new CBCCipherProvider(apiKey));
    }

    @VisibleForTesting
    public CBCCipher(@NonNull CipherProvider cipherProvider) {
        mCipherProvider = cipherProvider;
    }

    @Override
    @Nullable
    public String encrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = mCipherProvider.getEncryptionCipher();
        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(data.getBytes(CHARSET));
        } catch (Exception e) {
            Logger.e("Error encrypting data: " + e.getMessage());
            return null;
        } finally {
            mCipherProvider.release(cipher);
        }

        return Base64Util.encode(encryptedBytes);
    }

    @Override
    @Nullable
    public String decrypt(String data) {
        if (data == null) {
            return null;
        }

        Cipher cipher = mCipherProvider.getDecryptionCipher();
        try {
            byte[] bytes = cipher.doFinal(Base64Util.bytesDecode(data));

            return new String(bytes, CHARSET);
        } catch (Exception e) {
            Logger.e("Error decrypting data: " + e.getMessage());
            return null;
        } finally {
            mCipherProvider.release(cipher);
        }
    }
}
