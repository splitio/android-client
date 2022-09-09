package io.split.android.client.storage;

import androidx.annotation.NonNull;

public interface StoragePusher<T> {
    void push(@NonNull T element);
}
