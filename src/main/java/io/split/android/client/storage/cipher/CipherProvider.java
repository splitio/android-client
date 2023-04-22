package io.split.android.client.storage.cipher;

import androidx.annotation.Nullable;

import javax.crypto.Cipher;

public interface CipherProvider {

    @Nullable
    Cipher getEncryptionCipher();

    @Nullable
    Cipher getDecryptionCipher();

    void release(Cipher cipher);
}
