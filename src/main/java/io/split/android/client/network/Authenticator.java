package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface Authenticator<T extends AuthenticatedRequest<?>> {

    @Nullable T authenticate(@NonNull T request);
}
