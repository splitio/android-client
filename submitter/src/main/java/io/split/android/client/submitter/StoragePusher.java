package io.split.android.client.submitter;

import androidx.annotation.NonNull;

public interface StoragePusher<T> {
    void push(@NonNull T element);
}
