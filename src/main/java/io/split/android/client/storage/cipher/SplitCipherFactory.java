package io.split.android.client.storage.cipher;

public class SplitCipherFactory {

    public static SplitCipher create(String apiKey, boolean encryptionEnabled) {
        if (encryptionEnabled) {
            try {
                return new CBCCipher(apiKey);
            } catch (Exception e) {
                return null;
            }
        }

        return new NoOpCipher();
    }

    public static SplitCipher create(String apiKey, SplitEncryptionLevel fromLevel) {
        if (fromLevel == SplitEncryptionLevel.AES_128_CBC) {
            try {
                return new CBCCipher(apiKey);
            } catch (Exception e) {
                return null;
            }
        }
        return new NoOpCipher();
    }
}
