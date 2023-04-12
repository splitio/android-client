package io.split.android.client.storage.cipher;

public class SplitCipherFactory {

    public static SplitCipher create(String apiKey, boolean encryptionEnabled) {
        if (encryptionEnabled) {
            try {
                return new CBCCipher(apiKey);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing SplitCipher", e);
            }
        } else {
            return new NoOpCipher();
        }
    }
}
