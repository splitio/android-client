package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;

public interface HttpStreamResponse extends BaseHttpResponse {
    @Nullable
    BufferedReader getBufferedReader();
}
