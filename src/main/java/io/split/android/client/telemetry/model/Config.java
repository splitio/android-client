package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Config {

    @SerializedName("oM")
    private final int operationMode = OperationMode.STANDALONE.getNumericValue();

    @SerializedName("st")
    private final String storage = "memory";

    @SerializedName("sE")
    private boolean streamingEnabled;

    @SerializedName("rR")
    private RefreshRates refreshRates;

    @SerializedName("uO")
    private UrlOverrides urlOverrides;

    @SerializedName("iQ")
    private long impressionsQueueSize;

    @SerializedName("eQ")
    private long eventsQueueSize;

    @SerializedName("iM")
    private ImpressionsMode impressionsMode;

    @SerializedName("iL")
    private boolean impressionsListenerEnabled;

    @SerializedName("hP")
    private boolean httpProxyDetected;

    @SerializedName("aF")
    private long activeFactories;

    @SerializedName("rF")
    private long redundantActiveFactories;

    @SerializedName("tR")
    private long timeUntilSDKReady;

    @SerializedName("tC")
    private long timeUntilSDKReadyFromCache;

    @SerializedName("nR")
    private long SDKNotReadyUsage;

    @SerializedName("t")
    private List<String> tags;

    @SerializedName("i")
    private List<String> integrations;

    public int getOperationMode() {
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

    public long getTimeUntilSDKReadyFromCache() {
        return timeUntilSDKReadyFromCache;
    }

    public void setTimeUntilSDKReadyFromCache(long timeUntilSDKReadyFromCache) {
        this.timeUntilSDKReadyFromCache = timeUntilSDKReadyFromCache;
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
