package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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

    @NonNull
    public static List<String> decryptAll(@NonNull SplitCipher cipher, @NonNull List<String> values) {
        if (cipher instanceof CBCCipher) {
            return ((CBCCipher) cipher).decryptAll(values);
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            result.add(cipher.decrypt(value));
        }
        return result;
    }
}
