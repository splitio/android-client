package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

public class SplitAuthenticatedRequest implements AuthenticatedRequest<Request> {

    private final int mStatusCode;
    private Request mRequest;

    SplitAuthenticatedRequest(Response response) {
        mStatusCode = response.code();
        mRequest = response.request();
    }

    @Override
    public void setHeader(@NonNull String name, @NonNull String value) {
        if (mRequest == null) {
            return;
        }

        mRequest = mRequest.newBuilder().header(name, value).build();
    }

    @Nullable
    @Override
    public String getHeader(@NonNull String name) {
        if (mRequest == null) {
            return null;
        }

        return mRequest.header(name);
    }

    @Nullable
    @Override
    public Map<String, List<String>> getHeaders() {
        if (mRequest == null) {
            return null;
        }

        return mRequest.headers().toMultimap();
    }

    @Override
    public int getStatusCode() {
        return mStatusCode;
    }

    @Nullable
    @Override
    public String getRequestUrl() {
        if (mRequest == null) {
            return null;
        }

        return mRequest.url().toString();
    }
}
