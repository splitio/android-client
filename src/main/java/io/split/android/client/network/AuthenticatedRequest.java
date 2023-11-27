package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface AuthenticatedRequest<T> {

    void setHeader(@NonNull String name, @NonNull String value);

    @Nullable
    String getHeader(@NonNull String name);

    @Nullable
    T getRequest();
}
