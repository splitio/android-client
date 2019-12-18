package io.split.android.client.service;

import androidx.annotation.NonNull;

public interface HttpRequestParser<T> {
    String parse(@NonNull T data);
}
