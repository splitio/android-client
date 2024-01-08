package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

interface AuthenticatedRequest<T> {

    void setHeader(@NonNull String name, @NonNull String value);

    @Nullable
    String getHeader(@NonNull String name);

    @Nullable
    Map<String, String> getHeaders();

    @Nullable
    String getRequestUrl();
}
