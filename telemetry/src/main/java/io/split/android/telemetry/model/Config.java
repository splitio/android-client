package io.split.android.telemetry.model;

import java.util.List;

public class Config {

    private final OperationMode operationMode = OperationMode.STANDALONE;

    private final String storage = "memory";

    private boolean streamingEnabled;

    private RefreshRates refreshRates;

    private UrlOverrides urlOverrides;

    private long impressionsQueueSize;

    private long eventsQueueSize;

    private ImpressionsMode impressionsMode;

    private boolean impressionsListenerEnabled;

    private boolean httpProxyDetected;

    private long activeFactories;

    private long redundantActiveFactories;

    private long timeUntilSDKReady;

    private long BURTimeouts;

    private long SDKNotReadyUsage;

    private List<String> tags;

    private List<String> integrations;

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public String getStorage() {
        return storage;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }

    public RefreshRates getRefreshRates() {
        return refreshRates;
    }

    public void setRefreshRates(RefreshRates refreshRates) {
        this.refreshRates = refreshRates;
    }

    public UrlOverrides getUrlOverrides() {
        return urlOverrides;
    }

    public void setUrlOverrides(UrlOverrides urlOverrides) {
        this.urlOverrides = urlOverrides;
    }

    public long getImpressionsQueueSize() {
        return impressionsQueueSize;
    }

    public void setImpressionsQueueSize(long impressionsQueueSize) {
        this.impressionsQueueSize = impressionsQueueSize;
    }

    public long getEventsQueueSize() {
        return eventsQueueSize;
    }

    public void setEventsQueueSize(long eventsQueueSize) {
        this.eventsQueueSize = eventsQueueSize;
    }

    public ImpressionsMode getImpressionsMode() {
        return impressionsMode;
    }

    public void setImpressionsMode(ImpressionsMode impressionsMode) {
        this.impressionsMode = impressionsMode;
    }

    public boolean isImpressionsListenerEnabled() {
        return impressionsListenerEnabled;
    }

    public void setImpressionsListenerEnabled(boolean impressionsListenerEnabled) {
        this.impressionsListenerEnabled = impressionsListenerEnabled;
    }

    public boolean isHttpProxyDetected() {
        return httpProxyDetected;
    }

    public void setHttpProxyDetected(boolean httpProxyDetected) {
        this.httpProxyDetected = httpProxyDetected;
    }

    public long getActiveFactories() {
        return activeFactories;
    }

    public void setActiveFactories(long activeFactories) {
        this.activeFactories = activeFactories;
    }

    public long getRedundantActiveFactories() {
        return redundantActiveFactories;
    }

    public void setRedundantActiveFactories(long redundantActiveFactories) {
        this.redundantActiveFactories = redundantActiveFactories;
    }

    public long getTimeUntilSDKReady() {
        return timeUntilSDKReady;
    }

    public void setTimeUntilSDKReady(long timeUntilSDKReady) {
        this.timeUntilSDKReady = timeUntilSDKReady;
    }

    public long getBURTimeouts() {
        return BURTimeouts;
    }

    public void setBURTimeouts(long BURTimeouts) {
        this.BURTimeouts = BURTimeouts;
    }

    public long getSDKNotReadyUsage() {
        return SDKNotReadyUsage;
    }

    public void setSDKNotReadyUsage(long SDKNotReadyUsage) {
        this.SDKNotReadyUsage = SDKNotReadyUsage;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getIntegrations() {
        return integrations;
    }

    public void setIntegrations(List<String> integrations) {
        this.integrations = integrations;
    }
}

