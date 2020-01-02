package io.split.android.client.service.http;

import androidx.annotation.NonNull;

public interface HttpRequestParser<T> {
    String parse(@NonNull T data);
}
