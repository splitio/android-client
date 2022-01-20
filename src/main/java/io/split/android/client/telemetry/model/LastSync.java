package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class LastSync {

    @SerializedName("sp")
    private long lastSplitSync;

    @SerializedName("ms")
    private long lastMySegmentSync;

    @SerializedName("im")
    private long lastImpressionSync;

    @SerializedName("ic")
    private long lastImpressionCountSync;

    @SerializedName("ev")
    private long lastEventSync;

    @SerializedName("te")
    private long lastTelemetrySync;

    @SerializedName("to")
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

    public long getLastImpressionCountSync() {
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
