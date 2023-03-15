package io.split.android.client.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class SplitCipherImpl implements SplitCipher {
    private final static String CipherSettings = "AES/ECB/PKCS7Padding";
    private final static String ALGO = "AES";
    private final static int KEY_LENGTH = 32;

    @Override
    @Nullable
    public String encrypt(@NonNull String plainText, @NonNull String key) {

        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key);
        if (cipher == null) {
            return null;
        }

        try {
            byte[] plainTextBytes = plainText.getBytes();
            byte[] ciphertext = cipher.doFinal(plainTextBytes);
            return Base64Util.encode(ciphertext);
        } catch (IllegalBlockSizeException e) {
            Logger.v("Cypher IllegalBlockSizeException: " + e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            Logger.v("Cypher BadPaddingException: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.v("Cypher Unknown error: " + e.getLocalizedMessage());
        }

        return null;
    }

    @Override
    @Nullable
    public String decrypt(@NonNull String cipherText, @NonNull String key) {

        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
        if (cipher == null) {
            return null;
        }
        try {
            byte[] cipherBytes = Base64Util.bytesDecode(cipherText);
            byte[] plainTextBytes = cipher.doFinal(cipherBytes);
            return new String(plainTextBytes);
        } catch (IllegalBlockSizeException e) {
            Logger.v("Cypher IllegalBlockSizeException: " + e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            Logger.v("Cypher BadPaddingException: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.v("Could not create cipher");
        }
        return null;
    }

    private Cipher getCipher(int mode, String key) {
        try {
            Cipher cipher = null;
            SecretKeySpec secretKey = new SecretKeySpec(sanitizeKey(key).getBytes(), ALGO);
            cipher = Cipher.getInstance(CipherSettings);
            cipher.init(mode, secretKey);
            return cipher;

        } catch (NoSuchAlgorithmException e) {
            Logger.v("Could not create secret key: "+ e.getLocalizedMessage());
        } catch (NoSuchPaddingException e) {
            Logger.v("Could not create Cipher: " + e.getLocalizedMessage());
        } catch (InvalidKeyException e) {
            Logger.v("Cypher InvalidKeyException: " + e.getLocalizedMessage());
        }
        return null;
    }

    private String sanitizeKey(String key) {
        if (key.length() < KEY_LENGTH) {
            return keyFilled(key);
        } else if (key.length() > KEY_LENGTH) {
            return key.substring(0, KEY_LENGTH);
        }
        return key;
    }

    private String keyFilled(String key) {
        int fill = KEY_LENGTH - key.length();
        StringBuilder str = new StringBuilder(fill);
        for(int i=0; i<fill; i++) {
            str.append("0");
        }
        return key + str.toString();
    }
}
