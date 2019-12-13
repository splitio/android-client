package io.split.android.client.service;

import java.util.Map;

public interface HttpFetcher<T> {
    T execute() throws HttpFetcherException;
    T execute(Map<String, Object> params) throws HttpFetcherException;
}
