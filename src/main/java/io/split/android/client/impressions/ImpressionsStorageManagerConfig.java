package io.split.android.client.impressions;

public class ImpressionsStorageManagerConfig {
    private int _impressionsMaxSentAttempts = 0;
    private long _impressionsChunkOudatedTime = 0;

    public int getImpressionsMaxSentAttempts() {
        return _impressionsMaxSentAttempts;
    }

    public void setImpressionsMaxSentAttempts(int impressionsMaxSendAttempts) {
        this._impressionsMaxSentAttempts = impressionsMaxSendAttempts;
    }

    public long getImpressionsChunkOudatedTime() {
        return _impressionsChunkOudatedTime;
    }

    public void setImpressionsChunkOudatedTime(long impressionsChunkOudatedTime) {
        this._impressionsChunkOudatedTime = impressionsChunkOudatedTime;
    }
}
