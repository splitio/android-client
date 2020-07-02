package io.split.android.client.service.sseclient;

import io.split.android.client.lifecycle.SplitLifecycleAware;

public interface SseConnectionManager extends SplitLifecycleAware {
    void start();
    void stop();
    void setListener(SseConnectionManagerListener listener);
}
