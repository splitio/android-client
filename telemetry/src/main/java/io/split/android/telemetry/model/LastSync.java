package io.split.android.telemetry.model;

public class LastSync {

    private long lastSplitSync;

    private long lastSegmentSync;

    private long lastImpressionSync;

    private long lasImpressionCountSync;

    private long lastEventSync;

    private long lastTelemetrySync;

    private long lastTokenRefresh;

    public long getLastSplitSync() {
        return lastSplitSync;
    }

    public void setLastSplitSync(long lastSplitSync) {
        this.lastSplitSync = lastSplitSync;
    }

    public long getLastSegmentSync() {
        return lastSegmentSync;
    }

    public void setLastSegmentSync(long lastSegmentSync) {
        this.lastSegmentSync = lastSegmentSync;
    }

    public long getLastImpressionSync() {
        return lastImpressionSync;
    }

    public void setLastImpressionSync(long lastImpressionSync) {
        this.lastImpressionSync = lastImpressionSync;
    }

    public long getLasImpressionCountSync() {
        return lasImpressionCountSync;
    }

    public void setLasImpressionCountSync(long lasImpressionCountSync) {
        this.lasImpressionCountSync = lasImpressionCountSync;
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
