package io.split.android.telemetry.model;

import java.util.ArrayList;
import java.util.List;

public class HTTPLatencies {

    private List<Integer> splits;

    private List<Integer> segments;

    private List<Integer> impressions;

    private List<Integer> impressionsCount;

    private List<Integer> events;

    private List<Integer> telemetry;

    private List<Integer> token;

    public HTTPLatencies() {
        this.splits = new ArrayList<>();
        this.segments = new ArrayList<>();
        this.impressions = new ArrayList<>();
        this.impressionsCount = new ArrayList<>();
        this.events = new ArrayList<>();
        this.telemetry = new ArrayList<>();
        this.token = new ArrayList<>();
    }

    public List<Integer> getSplits() {
        return splits;
    }

    public void setSplits(List<Integer> splits) {
        this.splits = splits;
    }

    public List<Integer> getSegments() {
        return segments;
    }

    public void setSegments(List<Integer> segments) {
        this.segments = segments;
    }

    public List<Integer> getImpressions() {
        return impressions;
    }

    public void setImpressions(List<Integer> impressions) {
        this.impressions = impressions;
    }

    public List<Integer> getImpressionsCount() {
        return impressionsCount;
    }

    public void setImpressionsCount(List<Integer> impressionsCount) {
        this.impressionsCount = impressionsCount;
    }

    public List<Integer> getEvents() {
        return events;
    }

    public void setEvents(List<Integer> events) {
        this.events = events;
    }

    public List<Integer> getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(List<Integer> telemetry) {
        this.telemetry = telemetry;
    }

    public List<Integer> getToken() {
        return token;
    }

    public void setToken(List<Integer> token) {
        this.token = token;
    }
}
