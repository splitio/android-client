package io.split.android.client.telemetry.model;

import java.util.List;

public class HTTPLatencies {

    private List<Long> splits;

    private List<Long> segments;

    private List<Long> impressions;

    private List<Long> impressionsCount;

    private List<Long> events;

    private List<Long> telemetry;

    private List<Long> token;

    public List<Long> getSplits() {
        return splits;
    }

    public void setSplits(List<Long> splits) {
        this.splits = splits;
    }

    public List<Long> getSegments() {
        return segments;
    }

    public void setSegments(List<Long> segments) {
        this.segments = segments;
    }

    public List<Long> getImpressions() {
        return impressions;
    }

    public void setImpressions(List<Long> impressions) {
        this.impressions = impressions;
    }

    public List<Long> getImpressionsCount() {
        return impressionsCount;
    }

    public void setImpressionsCount(List<Long> impressionsCount) {
        this.impressionsCount = impressionsCount;
    }

    public List<Long> getEvents() {
        return events;
    }

    public void setEvents(List<Long> events) {
        this.events = events;
    }

    public List<Long> getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(List<Long> telemetry) {
        this.telemetry = telemetry;
    }

    public List<Long> getToken() {
        return token;
    }

    public void setToken(List<Long> token) {
        this.token = token;
    }
}
