package io.split.android.client.service.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.Map;
import java.util.Set;

public interface RepeatableParameterHttpFetcher<T> {
    T execute(@NonNull Set<Pair<String, Object>> params, @Nullable Map<String, String> headers) throws HttpFetcherException;
}
