package io.split.android.client.service.impressions;

import androidx.annotation.VisibleForTesting;

public class ImpressionManagerConfig {

    final long impressionsRefreshRate;
    final long impressionsCounterRefreshRate;
    final Mode impressionsMode;
    final int impressionsQueueSize;
    final long impressionsChunkSize;
    final long uniqueKeysRefreshRate;

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
        this.impressionsRefreshRate = impressionsRefreshRate;
        this.impressionsCounterRefreshRate = impressionsCounterRefreshRate;
        this.impressionsMode = impressionsMode;
        this.impressionsQueueSize = impressionsQueueSize;
        this.impressionsChunkSize = impressionsChunkSize;
        this.uniqueKeysRefreshRate = uniqueKeysRefreshRate;
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
