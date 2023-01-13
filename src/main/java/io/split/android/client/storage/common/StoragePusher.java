package io.split.android.client.storage.common;

import androidx.annotation.NonNull;

public interface StoragePusher<T> {
    void push(@NonNull T element);
}
