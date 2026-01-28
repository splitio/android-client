package io.split.android.client.service.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface HttpFetcher<T> {
    T execute(@NonNull Map<String, Object> params, @Nullable Map<String, String> headers) throws HttpFetcherException;
}
