package io.split.android.client.tracker;

import java.util.Map;

public interface Tracker {
    void enableTracking(boolean enable);

    boolean track(String key, String trafficType, String eventType, double value,
                  Map<String, Object> properties, boolean isSdkReady);
}
