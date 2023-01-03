package io.split.android.client;

import java.util.Map;

public interface EventsTracker {
    void enableTracking(boolean enable);
    boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties);
}