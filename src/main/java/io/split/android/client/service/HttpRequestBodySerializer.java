package io.split.android.client.service;

import androidx.annotation.NonNull;

public interface HttpRequestBodySerializer<T> {
    String serialize(@NonNull T data);
}
