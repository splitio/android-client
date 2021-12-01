package io.split.android.client.telemetry.model;

public class RefreshRates {

    private long splits;

    private long segments;

    private long impressions;

    private long events;

    private long telemetry;

    public long getSplits() {
        return splits;
    }

    public void setSplits(long splits) {
        this.splits = splits;
    }

    public long getSegments() {
        return segments;
    }

    public void setSegments(long segments) {
        this.segments = segments;
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
