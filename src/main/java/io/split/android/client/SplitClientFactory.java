package io.split.android.client;

import androidx.annotation.NonNull;

import io.split.android.client.api.Key;

public interface SplitClientFactory {

    SplitClient getClient(@NonNull Key key);
}
