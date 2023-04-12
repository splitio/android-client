package io.split.android.client.storage.cipher;

import androidx.annotation.Nullable;

public class SplitCipherImpl implements SplitCipher {

    private final SplitCipher mCipher;

    public SplitCipherImpl(String apiKey, boolean encryptionEnabled) {
        if (encryptionEnabled) {
            try {
                mCipher = new CBCCipher(apiKey);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing SplitCipher", e);
            }
        } else {
            mCipher = new NoOpCipher();
        }
    }

    @Nullable
    @Override
    public String encrypt(@Nullable String data) {
        return mCipher.encrypt(data);
    }

    @Nullable
    @Override
    public String decrypt(@Nullable String data) {
        return mCipher.decrypt(data);
    }
}
