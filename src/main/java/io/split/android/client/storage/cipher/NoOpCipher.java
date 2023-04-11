package io.split.android.client.storage.cipher;

public class NoOpCipher implements SplitCipher {

    @Override
    public String encrypt(String data) {
        return data;
    }

    @Override
    public String decrypt(String data) {
        return data;
    }
}
