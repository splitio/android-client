package io.split.android.client.service.impressions;

public class ImpressionsRecorderTaskConfig {
    final private int impressionsPerPush;
    public ImpressionsRecorderTaskConfig(int eventsPerPush) {
        this.impressionsPerPush = eventsPerPush;
    }

    public int getImpressionsPerPush() {
        return impressionsPerPush;
    }
}
