package io.split.android.client.telemetry.model;

public class RefreshRates {

    private long splits;

    private long mySegments;

    private long impressions;

    private long events;

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
