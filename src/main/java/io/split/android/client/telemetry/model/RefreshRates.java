package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class RefreshRates {

    @SerializedName("sp")
    private long splits;

    @SerializedName("ms")
    private long mySegments;

    @SerializedName("im")
    private long impressions;

    @SerializedName("ev")
    private long events;

    @SerializedName("te")
    private long telemetry;

    public long getSplits() {
        return splits;
    }

    public void setSplits(long splits) {
        this.splits = splits;
    }

    public long getMySegments() {
        return mySegments;
    }

    public void setMySegments(long mySegments) {
        this.mySegments = mySegments;
    }

    public long getImpressions() {
        return impressions;
    }

    public void setImpressions(long impressions) {
        this.impressions = impressions;
    }

    public long getEvents() {
        return events;
    }

    public void setEvents(long events) {
        this.events = events;
    }

    public long getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(long telemetry) {
        this.telemetry = telemetry;
    }
}
