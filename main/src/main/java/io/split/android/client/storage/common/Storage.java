package io.split.android.client.storage.common;

import androidx.annotation.NonNull;

public interface Storage<T> {
    void enablePersistence(boolean enabled);

    void push(@NonNull T element);

    void clearInMemory();
}
