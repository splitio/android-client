package io.split.android.client.tracker;

import java.util.Map;

public interface Tracker {
    void enableTracking(boolean enable);

    boolean track(String key, String trafficType, String eventType, Double value,
                  Map<String, Object> properties, boolean isSdkReady);
}
