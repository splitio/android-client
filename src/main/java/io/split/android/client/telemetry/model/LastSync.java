package io.split.android.client.telemetry.model;

public class LastSync {

    private long lastSplitSync;

    private long lastMySegmentSync;

    private long lastImpressionSync;

    private long lastImpressionCountSync;

    private long lastEventSync;

    private long lastTelemetrySync;

    private long lastTokenRefresh;

    public long getLastSplitSync() {
        return lastSplitSync;
    }

    public void setLastSplitSync(long lastSplitSync) {
        this.lastSplitSync = lastSplitSync;
    }

    public long getLastMySegmentSync() {
        return lastMySegmentSync;
    }

    public void setLastMySegmentSync(long lastMySegmentSync) {
        this.lastMySegmentSync = lastMySegmentSync;
    }

    public long getLastImpressionSync() {
        return lastImpressionSync;
    }

    public void setLastImpressionSync(long lastImpressionSync) {
        this.lastImpressionSync = lastImpressionSync;
    }

    public long getLasImpressionCountSync() {
        return lastImpressionCountSync;
    }

    public void setLastImpressionCountSync(long lasImpressionCountSync) {
        this.lastImpressionCountSync = lasImpressionCountSync;
    }

    public long getLastEventSync() {
        return lastEventSync;
    }

    public void setLastEventSync(long lastEventSync) {
        this.lastEventSync = lastEventSync;
    }

    public long getLastTelemetrySync() {
        return lastTelemetrySync;
    }

    public void setLastTelemetrySync(long lastTelemetrySync) {
        this.lastTelemetrySync = lastTelemetrySync;
    }

    public long getLastTokenRefresh() {
        return lastTokenRefresh;
    }

    public void setLastTokenRefresh(long lastTokenRefresh) {
        this.lastTokenRefresh = lastTokenRefresh;
    }
}
