package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.Closeable;

public interface HttpStreamResponse extends BaseHttpResponse, Closeable {
    @Nullable
    BufferedReader getBufferedReader();
}
