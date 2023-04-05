package io.split.android.client.storage.cipher.provider;

import androidx.annotation.NonNull;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

public interface KeyProvider {

    @Nullable
    SecretKey getKey(@NonNull String alias);
}
