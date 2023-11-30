package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SplitAuthenticatedRequest implements AuthenticatedRequest<HttpURLConnection> {

    private final String mUrl;
    private final Map<String, String> mHeaders = new ConcurrentHashMap<>();

    SplitAuthenticatedRequest(HttpURLConnection connection) {
        mUrl = (connection != null && connection.getURL() != null) ? connection.getURL().toString() : null;
    }

    @Override
    public void setHeader(@NonNull String name, @NonNull String value) {
        mHeaders.put(name, value);
    }

    @Nullable
    @Override
    public String getHeader(@NonNull String name) {
        return mHeaders.get(name);
    }

    @Nullable
    @Override
    public Map<String, String> getHeaders() {
        return new HashMap<>(mHeaders);
    }

    @Nullable
    @Override
    public String getRequestUrl() {
        return mUrl;
    }
}
