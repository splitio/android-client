package io.split.android.client.common;

public interface SplitCipher {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
