package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class LastSync {

    @SerializedName("sp")
    private Long lastSplitSync;

    @SerializedName("ms")
    private Long lastMySegmentSync;

    @SerializedName("mls")
    private Long lastMyLargeSegmentSync;

    @SerializedName("im")
    private Long lastImpressionSync;

    @SerializedName("ic")
    private Long lastImpressionCountSync;

    @SerializedName("ev")
    private Long lastEventSync;

    @SerializedName("te")
    private Long lastTelemetrySync;

    @SerializedName("to")
    private Long lastTokenRefresh;

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

    public long getLastMyLargeSegmentSync() {
        return lastMyLargeSegmentSync;
    }

    public void setLastMyLargeSegmentSync(long lastMyLargeSegmentSync) {
        this.lastMyLargeSegmentSync = lastMyLargeSegmentSync;
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
