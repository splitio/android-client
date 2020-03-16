package io.split.android.client.service.sseclient;

import androidx.annotation.NonNull;

import java.util.Map;

import io.split.android.client.service.executor.SplitTaskExecutor;

import static androidx.core.util.Preconditions.checkNotNull;

public class PushManager {

    private final SseClient mSseClient;
    private final SplitTaskExecutor mTaskExecutor;

    public PushManager(@NonNull SseClient sseClient,
                       @NonNull SplitTaskExecutor taskExecutor) {
        mSseClient = checkNotNull(sseClient);
        mTaskExecutor = checkNotNull(taskExecutor);
    }

    public void start() {
        // TODO: Should add a connect method to sseClient to connect from here
    }

    public void stop() {
        mSseClient.disconnect();
    }

    private class SseMessageHandler implements SseClientListener {

        @Override
        public void onOpen() {

        }

        @Override
        public void onMessage(Map<String, String> values) {

        }

        @Override
        public void onError() {

        }
    }
}
