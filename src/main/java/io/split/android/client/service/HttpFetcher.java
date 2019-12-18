package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.Map;

public interface HttpFetcher<T> {
    T execute(@NonNull Map<String, Object> params) throws HttpFetcherException;
}
