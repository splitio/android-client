package io.split.android.client.service.http;

import androidx.annotation.NonNull;

public interface HttpRecorder<T> {
    void execute(@NonNull T data) throws HttpRecorderException;
}
