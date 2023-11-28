package io.split.android.client.network;

import androidx.annotation.NonNull;

interface InternalAuthenticatorFactory<T> {

    @NonNull T getAuthenticator(SplitAuthenticator splitAuthenticator);
}
