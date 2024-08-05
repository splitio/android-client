package io.split.android.client.telemetry.model;

import androidx.annotation.VisibleForTesting;

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

    @SerializedName("lseC")
    private long largeSegmentCount;

    @SerializedName("skC")
    private final long segmentKeyCount = 1;

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

    @SerializedName("ufs")
    private UpdatesFromSSE updatesFromSSE;

    public void setLastSynchronizations(LastSync lastSynchronizations) {
        this.lastSynchronizations = lastSynchronizations;
    }

    public void setMethodLatencies(MethodLatencies methodLatencies) {
        this.methodLatencies = methodLatencies;
    }

    public void setMethodExceptions(MethodExceptions methodExceptions) {
        this.methodExceptions = methodExceptions;
    }

    public void setHttpErrors(HttpErrors httpErrors) {
        this.httpErrors = httpErrors;
    }

    public void setHttpLatencies(HttpLatencies httpLatencies) {
        this.httpLatencies = httpLatencies;
    }

    public void setTokenRefreshes(long tokenRefreshes) {
        this.tokenRefreshes = tokenRefreshes;
    }

    public void setAuthRejections(long authRejections) {
        this.authRejections = authRejections;
    }

    public void setImpressionsQueued(long impressionsQueued) {
        this.impressionsQueued = impressionsQueued;
    }

    public void setImpressionsDeduped(long impressionsDeduped) {
        this.impressionsDeduped = impressionsDeduped;
    }

    public void setImpressionsDropped(long impressionsDropped) {
        this.impressionsDropped = impressionsDropped;
    }

    public void setSplitCount(long splitCount) {
        this.splitCount = splitCount;
    }

    public void setSegmentCount(long segmentCount) {
        this.segmentCount = segmentCount;
    }

    public void setLargeSegmentCount(long largeSegmentCount) {
        this.largeSegmentCount = largeSegmentCount;
    }

    public void setSessionLengthMs(long sessionLengthMs) {
        this.sessionLengthMs = sessionLengthMs;
    }

    public void setEventsQueued(long eventsQueued) {
        this.eventsQueued = eventsQueued;
    }

    public void setEventsDropped(long eventsDropped) {
        this.eventsDropped = eventsDropped;
    }

    public void setStreamingEvents(List<StreamingEvent> streamingEvents) {
        this.streamingEvents = streamingEvents;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setUpdatesFromSSE(UpdatesFromSSE updatesFromSSE) {
        this.updatesFromSSE = updatesFromSSE;
    }

    public List<String> getTags() {
        return tags;
    }

    public UpdatesFromSSE getUpdatesFromSSE() {
        return updatesFromSSE;
    }

    @VisibleForTesting
    public LastSync getLastSynchronizations() {
        return lastSynchronizations;
    }

    @VisibleForTesting
    public MethodLatencies getMethodLatencies() {
        return methodLatencies;
    }

    @VisibleForTesting
    public MethodExceptions getMethodExceptions() {
        return methodExceptions;
    }

    @VisibleForTesting
    public HttpErrors getHttpErrors() {
        return httpErrors;
    }

    @VisibleForTesting
    public HttpLatencies getHttpLatencies() {
        return httpLatencies;
    }

    @VisibleForTesting
    public long getTokenRefreshes() {
        return tokenRefreshes;
    }

    @VisibleForTesting
    public long getAuthRejections() {
        return authRejections;
    }

    @VisibleForTesting
    public long getImpressionsQueued() {
        return impressionsQueued;
    }

    @VisibleForTesting
    public long getImpressionsDeduped() {
        return impressionsDeduped;
    }

    @VisibleForTesting
    public long getImpressionsDropped() {
        return impressionsDropped;
    }

    @VisibleForTesting
    public long getSplitCount() {
        return splitCount;
    }

    @VisibleForTesting
    public long getSegmentCount() {
        return segmentCount;
    }

    @VisibleForTesting
    public long getLargeSegmentCount() {
        return largeSegmentCount;
    }

    @VisibleForTesting
    public long getSessionLengthMs() {
        return sessionLengthMs;
    }

    @VisibleForTesting
    public long getEventsQueued() {
        return eventsQueued;
    }

    @VisibleForTesting
    public long getEventsDropped() {
        return eventsDropped;
    }

    @VisibleForTesting
    public List<StreamingEvent> getStreamingEvents() {
        return streamingEvents;
    }
}
