package io.split.android.client.common;

public class SplitCipherNop implements SplitCipher {
    @Override
    public String encrypt(String plainText) {
        return plainText;
    }

    @Override
    public String decrypt(String cipherText) {
        return cipherText;
    }
}
