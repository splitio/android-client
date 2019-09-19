package io.split.android.client.impressions;

public class ImpressionsManagerConfig {
    private long chunkSize;
    private int waitBeforeShutdown;
    private int queueSize;
    private int refreshRate;
    private String endpoint;

    public ImpressionsManagerConfig(long chunkSize, int waitBeforeShutdown, int queueSize, int refreshRate, String endpoint) {
        this.chunkSize = chunkSize;
        this.waitBeforeShutdown = waitBeforeShutdown;
        this.queueSize = queueSize;
        this.refreshRate = refreshRate;
        this.endpoint = endpoint;
    }

    public long chunkSize() {
        return chunkSize;
    }

    public int waitBeforeShutdown() {
        return waitBeforeShutdown;
    }

    public int queueSize() {
        return queueSize;
    }

    public int refreshRate() {
        return refreshRate;
    }

    public String endpoint() {
        return endpoint;
    }
}
