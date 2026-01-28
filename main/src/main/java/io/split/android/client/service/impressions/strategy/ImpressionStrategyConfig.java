package io.split.android.client.service.impressions.strategy;

public final class ImpressionStrategyConfig {

    private final int mImpressionsQueueSize;
    private final long mImpressionsChunkSize;
    private final int mImpressionsRefreshRate;
    private final int mImpressionsCounterRefreshRate;
    private final int mUniqueKeysRefreshRate;
    private final boolean mUserConsentIsGranted;
    private final long mDedupeTimeIntervalInMs;

    public ImpressionStrategyConfig(int impressionsQueueSize,
                                    long impressionsChunkSize,
                                    int impressionsRefreshRate,
                                    int impressionsCounterRefreshRate,
                                    int uniqueKeysRefreshRate,
                                    boolean userConsentIsGranted,
                                    long dedupeTimeIntervalInMs) {
        mImpressionsQueueSize = impressionsQueueSize;
        mImpressionsChunkSize = impressionsChunkSize;
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mUniqueKeysRefreshRate = uniqueKeysRefreshRate;
        mUserConsentIsGranted = userConsentIsGranted;
        mDedupeTimeIntervalInMs = dedupeTimeIntervalInMs;
    }

    public int getImpressionsQueueSize() {
        return mImpressionsQueueSize;
    }

    public long getImpressionsChunkSize() {
        return mImpressionsChunkSize;
    }

    public int getImpressionsRefreshRate() {
        return mImpressionsRefreshRate;
    }

    public int getImpressionsCounterRefreshRate() {
        return mImpressionsCounterRefreshRate;
    }

    public int getUniqueKeysRefreshRate() {
        return mUniqueKeysRefreshRate;
    }

    public boolean isUserConsentGranted() {
        return mUserConsentIsGranted;
    }

    public long getDedupeTimeIntervalInMs() {
        return mDedupeTimeIntervalInMs;
    }
}
