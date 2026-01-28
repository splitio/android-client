package io.split.android.client.service.http;

import androidx.annotation.NonNull;

public interface HttpRequestBodySerializer<T> {
    String serialize(@NonNull T data);
}
