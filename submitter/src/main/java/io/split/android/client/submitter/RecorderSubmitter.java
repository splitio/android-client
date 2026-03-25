package io.split.android.client.submitter;

import androidx.annotation.NonNull;

public interface RecorderSubmitter<T> {
    void execute(@NonNull T data) throws RecorderException;
}
