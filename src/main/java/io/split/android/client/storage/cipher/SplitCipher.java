package io.split.android.client.storage.cipher;

import javax.annotation.Nullable;

public interface SplitCipher {

    @Nullable
    String encrypt(@Nullable String data);

    @Nullable
    String decrypt(@Nullable String data);
}