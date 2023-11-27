package io.split.android.client.network;

import androidx.annotation.Nullable;

public interface Authenticator<T extends AuthenticatedRequest<?>> {

    @Nullable T authenticate(T request);
}
