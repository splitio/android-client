package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public class Stats {

    @SerializedName("lS")
    private LastSync lastSynchronizations;

    @SerializedName("mL")
    private MethodLatencies methodLatencies;

    @SerializedName("mE")
    private MethodExceptions methodExceptions;

    @SerializedName("hE")
    private HttpErrors httpErrors;

    @SerializedName("hL")
    private HttpLatencies httpLatencies;

    @SerializedName("tR")
    private long tokenRefreshes;

    @SerializedName("aR")
    private long authRejections;

    @SerializedName("iQ")
    private long impressionsQueued;

    @SerializedName("iDe")
    private long impressionsDeduped;

    @SerializedName("iDr")
    private long impressionsDropped;

    @SerializedName("spC")
    private long splitCount;

    @SerializedName("seC")
    private long segmentCount;

    @SerializedName("skC")
    private long segmentKeyCount;

    @SerializedName("sL")
    private long sessionLengthMs;

    @SerializedName("eQ")
    private long eventsQueued;

    @SerializedName("eD")
    private long eventsDropped;

    @SerializedName("sE")
    private List<StreamingEvent> streamingEvents;

    @SerializedName("t")
    private List<String> tags;

    public LastSync getLastSynchronizations() {
        return lastSynchronizations;
    }

    public void setLastSynchronizations(LastSync lastSynchronizations) {
        this.lastSynchronizations = lastSynchronizations;
    }

    public MethodLatencies getMethodLatencies() {
        return methodLatencies;
    }

    public void setMethodLatencies(MethodLatencies methodLatencies) {
        this.methodLatencies = methodLatencies;
    }

    public MethodExceptions getMethodExceptions() {
        return methodExceptions;
    }

    public void setMethodExceptions(MethodExceptions methodExceptions) {
        this.methodExceptions = methodExceptions;
    }

    public HttpErrors getHttpErrors() {
        return httpErrors;
    }

    public void setHttpErrors(HttpErrors httpErrors) {
        this.httpErrors = httpErrors;
    }

    public HttpLatencies getHttpLatencies() {
        return httpLatencies;
    }

    public void setHttpLatencies(HttpLatencies httpLatencies) {
        this.httpLatencies = httpLatencies;
    }

    public long getTokenRefreshes() {
        return tokenRefreshes;
    }

    public void setTokenRefreshes(long tokenRefreshes) {
        this.tokenRefreshes = tokenRefreshes;
    }

    public long getAuthRejections() {
        return authRejections;
    }

    public void setAuthRejections(long authRejections) {
        this.authRejections = authRejections;
    }

    public long getImpressionsQueued() {
        return impressionsQueued;
    }

    public void setImpressionsQueued(long impressionsQueued) {
        this.impressionsQueued = impressionsQueued;
    }

    public long getImpressionsDeduped() {
        return impressionsDeduped;
    }

    public void setImpressionsDeduped(long impressionsDeduped) {
        this.impressionsDeduped = impressionsDeduped;
    }

    public long getImpressionsDropped() {
        return impressionsDropped;
    }

    public void setImpressionsDropped(long impressionsDropped) {
        this.impressionsDropped = impressionsDropped;
    }

    public long getSplitCount() {
        return splitCount;
    }

    public void setSplitCount(long splitCount) {
        this.splitCount = splitCount;
    }

    public long getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(long segmentCount) {
        this.segmentCount = segmentCount;
    }

    public long getSegmentKeyCount() {
        return segmentKeyCount;
    }

    public void setSegmentKeyCount(long segmentKeyCount) {
        this.segmentKeyCount = segmentKeyCount;
    }

    public long getSessionLengthMs() {
        return sessionLengthMs;
    }

    public void setSessionLengthMs(long sessionLengthMs) {
        this.sessionLengthMs = sessionLengthMs;
    }

    public long getEventsQueued() {
        return eventsQueued;
    }

    public void setEventsQueued(long eventsQueued) {
        this.eventsQueued = eventsQueued;
    }

    public long getEventsDropped() {
        return eventsDropped;
    }

    public void setEventsDropped(long eventsDropped) {
        this.eventsDropped = eventsDropped;
    }

    public List<StreamingEvent> getStreamingEvents() {
        return streamingEvents;
    }

    public void setStreamingEvents(List<StreamingEvent> streamingEvents) {
        this.streamingEvents = streamingEvents;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
