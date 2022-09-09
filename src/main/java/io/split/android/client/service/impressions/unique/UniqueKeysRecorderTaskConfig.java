package io.split.android.client.service.impressions.unique;

public class UniqueKeysRecorderTaskConfig {

    private final int mElementsPerPush;
    private final long mEstimatedSizeInBytes;

    public UniqueKeysRecorderTaskConfig(int elementsPerPush, long estimatedSizeInByes) {
        mElementsPerPush = elementsPerPush;
        mEstimatedSizeInBytes = estimatedSizeInByes;
    }

    public int getElementsPerPush() {
        return mElementsPerPush;
    }

    public long getEstimatedSizeInBytes() {
        return mEstimatedSizeInBytes;
    }
}
