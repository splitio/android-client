package io.split.android.client.common;

public interface SplitCipher {
    String encrypt(String plainText, String key);
    String decrypt(String cipherText, String key);
}
