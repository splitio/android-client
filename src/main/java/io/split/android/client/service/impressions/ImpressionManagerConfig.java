package io.split.android.client.service.impressions;

import androidx.annotation.VisibleForTesting;

public class ImpressionManagerConfig {

    private final long mImpressionsRefreshRate;
    private final long mImpressionsCounterRefreshRate;
    private final Mode mImpressionsMode;
    private final int mImpressionsQueueSize;
    private final long mImpressionsChunkSize;
    private final long mUniqueKeysRefreshRate;

    public ImpressionManagerConfig(long impressionsRefreshRate,
                                   long impressionsCounterRefreshRate,
                                   ImpressionsMode impressionsMode,
                                   int impressionsQueueSize,
                                   long impressionsChunkSize,
                                   long uniqueKeysRefreshRate) {
        this(impressionsRefreshRate,
                impressionsCounterRefreshRate,
                Mode.fromImpressionMode(impressionsMode),
                impressionsQueueSize,
                impressionsChunkSize,
                uniqueKeysRefreshRate);
    }

    @VisibleForTesting
    public ImpressionManagerConfig(long impressionsRefreshRate,
                                   long impressionsCounterRefreshRate,
                                   Mode impressionsMode,
                                   int impressionsQueueSize,
                                   long impressionsChunkSize,
                                   long uniqueKeysRefreshRate) {
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mImpressionsMode = impressionsMode;
        mImpressionsQueueSize = impressionsQueueSize;
        mImpressionsChunkSize = impressionsChunkSize;
        mUniqueKeysRefreshRate = uniqueKeysRefreshRate;
    }

    public long getImpressionsRefreshRate() {
        return mImpressionsRefreshRate;
    }

    public long getImpressionsCounterRefreshRate() {
        return mImpressionsCounterRefreshRate;
    }

    public Mode getImpressionsMode() {
        return mImpressionsMode;
    }

    public int getImpressionsQueueSize() {
        return mImpressionsQueueSize;
    }

    public long getImpressionsChunkSize() {
        return mImpressionsChunkSize;
    }

    public long getUniqueKeysRefreshRate() {
        return mUniqueKeysRefreshRate;
    }

    public enum Mode {
        OPTIMIZED,
        DEBUG,
        NONE;

        public static Mode fromImpressionMode(ImpressionsMode mode) {
            if (mode == ImpressionsMode.DEBUG) {
                return Mode.DEBUG;
            } else {
                return Mode.OPTIMIZED;
            }
        }

        public boolean isDebug() {
            return this == Mode.DEBUG;
        }

        public boolean isNone() {
            return this == Mode.NONE;
        }

        public boolean isOptimized() {
            return this == Mode.OPTIMIZED;
        }
    }
}
