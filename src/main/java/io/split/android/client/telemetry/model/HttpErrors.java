package io.split.android.client.telemetry.model;

import java.util.Map;

public class HttpErrors {

    private Map<Long, Long> splitSyncErrs;

    private Map<Long, Long> segmentSyncErrs;

    private Map<Long, Long> impressionSyncErrs;

    private Map<Long, Long> impressionCountSyncErrs;

    private Map<Long, Long> eventsSyncErrs;

    private Map<Long, Long> telemetrySyncErrs;

    private Map<Long, Long> tokenGetErrs;

    public Map<Long, Long> getSplitSyncErrs() {
        return splitSyncErrs;
    }

    public void setSplitSyncErrs(Map<Long, Long> splitSyncErrs) {
        this.splitSyncErrs = splitSyncErrs;
    }

    public Map<Long, Long> getSegmentSyncErrs() {
        return segmentSyncErrs;
    }

    public void setSegmentSyncErrs(Map<Long, Long> segmentSyncErrs) {
        this.segmentSyncErrs = segmentSyncErrs;
    }

    public Map<Long, Long> getImpressionSyncErrs() {
        return impressionSyncErrs;
    }

    public void setImpressionSyncErrs(Map<Long, Long> impressionSyncErrs) {
        this.impressionSyncErrs = impressionSyncErrs;
    }

    public Map<Long, Long> getImpressionCountSyncErrs() {
        return impressionCountSyncErrs;
    }

    public void setImpressionCountSyncErrs(Map<Long, Long> impressionCountSyncErrs) {
        this.impressionCountSyncErrs = impressionCountSyncErrs;
    }

    public Map<Long, Long> getEventsSyncErrs() {
        return eventsSyncErrs;
    }

    public void setEventsSyncErrs(Map<Long, Long> eventsSyncErrs) {
        this.eventsSyncErrs = eventsSyncErrs;
    }

    public Map<Long, Long> getTelemetrySyncErrs() {
        return telemetrySyncErrs;
    }

    public void setTelemetrySyncErrs(Map<Long, Long> telemetrySyncErrs) {
        this.telemetrySyncErrs = telemetrySyncErrs;
    }

    public Map<Long, Long> getTokenGetErrs() {
        return tokenGetErrs;
    }

    public void setTokenGetErrs(Map<Long, Long> tokenGetErrs) {
        this.tokenGetErrs = tokenGetErrs;
    }
}
