package io.split.android.client.tracker;

import java.util.Map;

/**
 * Domain object representing a track event inside the tracker module.
 * This is intentionally separate from the networking DTO (Event) used in main/.
 */
public class TrackerEvent {
    public String trafficType;
    public String eventType;
    public String key;
    public Double value;
    public long timestamp;
    public Map<String, Object> properties;
    public int sizeInBytes;
}
