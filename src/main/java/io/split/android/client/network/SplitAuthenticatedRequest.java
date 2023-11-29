package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitAuthenticatedRequest implements AuthenticatedRequest<HttpURLConnection> {

    private final HttpURLConnection mRequest;

    SplitAuthenticatedRequest(HttpURLConnection request) {
        mRequest = request;
    }

    @Override
    public void setHeader(@NonNull String name, @NonNull String value) {
        if (mRequest == null) {
            return;
        }

        mRequest.setRequestProperty(name, value);
    }

    @Nullable
    @Override
    public String getHeader(@NonNull String name) {
        if (mRequest == null) {
            return null;
        }

        return mRequest.getRequestProperty(name);
    }

    @Nullable
    @Override
    public Map<String, List<String>> getHeaders() {
        if (mRequest == null) {
            return null;
        }

        return new HashMap<>(mRequest.getHeaderFields());
    }

    @Override
    public int getStatusCode() {
        try {
            if (mRequest == null) {
                return -1;
            }

            return mRequest.getResponseCode();
        } catch (IOException e) {
            return -1;
        }
    }

    @Nullable
    @Override
    public String getRequestUrl() {
        if (mRequest == null || mRequest.getURL() == null) {
            return null;
        }

        return mRequest.getURL().toString();
    }
}
