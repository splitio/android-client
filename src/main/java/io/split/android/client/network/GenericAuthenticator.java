package io.split.android.client.network;

import androidx.annotation.NonNull;

interface GenericAuthenticator<T extends AuthenticatedRequest<?>> {

    @NonNull
    T authenticate(T request);
}
