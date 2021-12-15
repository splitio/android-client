package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HttpLatencies {

    @SerializedName("sp")
    private List<Long> splits;

    @SerializedName("ms")
    private List<Long> mySegments;

    @SerializedName("im")
    private List<Long> impressions;

    @SerializedName("ic")
    private List<Long> impressionsCount;

    @SerializedName("ev")
    private List<Long> events;

    @SerializedName("te")
    private List<Long> telemetry;

    @SerializedName("to")
    private List<Long> token;

    public List<Long> getSplits() {
        return splits;
    }

    public void setSplits(List<Long> splits) {
        this.splits = splits;
    }

    public List<Long> getMySegments() {
        return mySegments;
    }

    public void setMySegments(List<Long> mySegments) {
        this.mySegments = mySegments;
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
