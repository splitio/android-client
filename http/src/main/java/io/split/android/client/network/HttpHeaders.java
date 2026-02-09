package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpHeaders {

    private final Map<String, String> mHeaders;

    public HttpHeaders() {
        mHeaders = new LinkedHashMap<>();
    }

    public HttpHeaders(@NonNull Map<String, String> headers) {
        mHeaders = new LinkedHashMap<>(headers);
    }

    public void set(@NonNull String name, @NonNull String value) {
        mHeaders.put(name, value);
    }

    @Nullable
    public String get(@NonNull String name) {
        return mHeaders.get(name);
    }

    @NonNull
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(mHeaders);
    }

    public boolean isEmpty() {
        return mHeaders.isEmpty();
    }

    public int size() {
        return mHeaders.size();
    }
}
