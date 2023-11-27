package io.split.android.client.network;

import androidx.annotation.Nullable;

public interface AuthenticatedRequest<T> {

    void setHeader(String name, String value);

    @Nullable
    String getHeader(String name);

    T getRequest();
}
