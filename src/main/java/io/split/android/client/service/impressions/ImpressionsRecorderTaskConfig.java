package io.split.android.client.service.impressions;

public class ImpressionsRecorderTaskConfig {
    final private int impressionsPerPush;
    final private long estimatedSizeInBytes;

    public ImpressionsRecorderTaskConfig(int impressionsPerPush,
                                         long estimatedSizeInBytes) {
        this.impressionsPerPush = impressionsPerPush;
        this.estimatedSizeInBytes = estimatedSizeInBytes;
    }

    public int getImpressionsPerPush() {
        return impressionsPerPush;
    }

    public long getEstimatedSizeInBytes() {
        return estimatedSizeInBytes;
    }
}
