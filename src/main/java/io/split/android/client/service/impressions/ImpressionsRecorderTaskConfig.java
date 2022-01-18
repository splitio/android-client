package io.split.android.client.service.impressions;

public class ImpressionsRecorderTaskConfig {
    final private int impressionsPerPush;
    final private long estimatedSizeInBytes;
    final private boolean shouldRecordTelemetry;

    public ImpressionsRecorderTaskConfig(int impressionsPerPush,
                                         long estimatedSizeInBytes,
                                         boolean shouldRecordTelemetry) {
        this.impressionsPerPush = impressionsPerPush;
        this.estimatedSizeInBytes = estimatedSizeInBytes;
        this.shouldRecordTelemetry = shouldRecordTelemetry;
    }

    public int getImpressionsPerPush() {
        return impressionsPerPush;
    }

    public long getEstimatedSizeInBytes() {
        return estimatedSizeInBytes;
    }

    public boolean shouldRecordTelemetry() {
        return shouldRecordTelemetry;
    }
}
