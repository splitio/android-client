package io.split.android.client.network;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;

public interface HttpStreamResponse {

    boolean isSuccess();

    int getHttpStatus();

    @Nullable BufferedReader getBufferedReader();
}
