package io.split.android.client.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface Authenticator {

    @Nullable AuthenticatedRequest authenticate(@NonNull AuthenticatedRequest request);
}
