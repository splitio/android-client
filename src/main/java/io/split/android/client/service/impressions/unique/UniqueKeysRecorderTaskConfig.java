package io.split.android.client.service.impressions.unique;

public class UniqueKeysRecorderTaskConfig {

    private final int mElementsPerPush;
    private final int mMaxSizeInBytes;
    private final int mEstimatedSizeInBytes;

    public UniqueKeysRecorderTaskConfig(int elementsPerPush, int maxSizeInBytes, int estimatedSizeInByes) {
        mElementsPerPush = elementsPerPush;
        mMaxSizeInBytes = maxSizeInBytes;
        mEstimatedSizeInBytes = estimatedSizeInByes;
    }

    public int getElementsPerPush() {
        return mElementsPerPush;
    }

    public int getMaxSizeInBytes() {
        return mMaxSizeInBytes;
    }

    public int getEstimatedSizeInBytes() {
        return mEstimatedSizeInBytes;
    }
}
