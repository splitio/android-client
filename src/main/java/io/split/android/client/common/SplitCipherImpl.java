package io.split.android.client.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.logger.Logger;

public class SplitCipherImpl implements SplitCipher {

    public final static int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    public final static int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;

    private final static String CipherSettings = "AES/CBC/PKCS5Padding";
    private final static String ALGO = "AES";
    private final static int KEY_LENGTH = 16;

    private Cipher mCipher;

    private final SecretKeySpec mSecretKey;

    public SplitCipherImpl(int mode, String secretKey) {
        mSecretKey = new SecretKeySpec(sanitizeKey(secretKey).getBytes(), ALGO);
        mCipher = getCipher(mode);
    }

    @Override
    @Nullable
    public String encrypt(@NonNull String plainText) {

        if (mCipher == null) {
            return null;
        }

        try {
            byte[] plainTextBytes = plainText.getBytes();
            byte[] ciphertext = mCipher.doFinal(plainTextBytes);
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
    public String decrypt(String cipherText) {

        if (cipherText == null) {
            return null;
        }

        if (mCipher == null) {
            return null;
        }
        try {
            byte[] cipherBytes = Base64Util.bytesDecode(cipherText);
            byte[] plainTextBytes = mCipher.doFinal(cipherBytes);
            return new String(plainTextBytes);
        } catch (IllegalBlockSizeException e) {
            Logger.v("Cypher IllegalBlockSizeException: " + e.getLocalizedMessage());
        } catch (BadPaddingException e) {
            Logger.v("Cypher BadPaddingException: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.v("Could not create cipher: " + e.getLocalizedMessage());
        }
        return null;
    }

    private Cipher getCipher(int mode) {
        try {
            Cipher cipher = null;
            cipher = Cipher.getInstance(CipherSettings);
            // TODO: Replace iv by part of the API_KEY
            IvParameterSpec ivP = new IvParameterSpec("pepepepepepepepe".getBytes());
            cipher.init(mode, mSecretKey, ivP);
            return cipher;

        } catch (NoSuchAlgorithmException e) {
            Logger.v("Could not create secret key: "+ e.getLocalizedMessage());
        } catch (NoSuchPaddingException e) {
            Logger.v("Could not create Cipher: " + e.getLocalizedMessage());
        } catch (InvalidKeyException e) {
            Logger.v("Cypher InvalidKeyException: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.v("Cypher WTF: " + e.getLocalizedMessage());
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
