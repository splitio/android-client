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
        mRequest = mRequest.newBuilder().header(name, value).build();
    }

    @Nullable
    @Override
    public String getHeader(@NonNull String name) {
        return mRequest.header(name);
    }

    @Nullable
    @Override
    public Map<String, List<String>> getHeaders() {
        return mRequest.headers().toMultimap();
    }

    @Override
    public int getStatusCode() {
        return mStatusCode;
    }

    @Override
    public String getRequestUrl() {
        return mRequest.url().toString();
    }
}
