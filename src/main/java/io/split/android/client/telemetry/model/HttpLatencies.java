package io.split.android.client.telemetry.model;

import java.util.List;

public class HttpLatencies {

    private List<Long> splits;

    private List<Long> mySegments;

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

    public List<Long> getMySegments() {
        return mySegments;
    }

    public void setMySegments(List<Long> mySegments) {
        this.mySegments = mySegments;
    }
}
