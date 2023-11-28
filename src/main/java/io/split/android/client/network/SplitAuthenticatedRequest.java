package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class SplitAuthenticatedRequest implements AuthenticatedRequest<Response> {

    private final Response mResponse;

    SplitAuthenticatedRequest(Response request) {
        mResponse = request;
    }

    @Override
    public void setHeader(@NonNull String name, @NonNull String value) {
        mResponse.newBuilder().header(name, value);
    }

    @Nullable
    @Override
    public String getHeader(@NonNull String name) {
        return mResponse.header(name);
    }

    @Nullable
    @Override
    public Map<String, List<String>> getHeaders() {
        return mResponse.headers().toMultimap();
    }

    @Nullable
    @Override
    public Response getRequest() {
        return mResponse;
    }

    @Override
    public int getStatusCode() {
        return mResponse.code();
    }

    @Override
    public String getRequestUrl() {
        return mResponse.request().url().toString();
    }
}
