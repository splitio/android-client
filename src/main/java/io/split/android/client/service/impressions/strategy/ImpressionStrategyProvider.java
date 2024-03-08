package io.split.android.client.service.impressions.strategy;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManagerRetryTimerProviderImpl;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.observer.ImpressionsObserverImpl;
import io.split.android.client.service.impressions.unique.UniqueKeysTrackerImpl;
import io.split.android.client.service.synchronizer.RecorderSyncHelperImpl;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class ImpressionStrategyProvider {

    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitStorageContainer mStorageContainer;
    private final ImpressionsTaskFactory mSplitTaskFactory;
    private final TelemetryRuntimeProducer mTelemetryStorage;
    private final int mImpressionsQueueSize;
    private final long mImpressionsChunkSize;
    private final int mImpressionsRefreshRate;
    private final int mImpressionsCounterRefreshRate;
    private final int mUniqueKeysRefreshRate;
    private final boolean mUserConsentIsGranted;

    public ImpressionStrategyProvider(SplitTaskExecutor splitTaskExecutor,
                                      SplitStorageContainer storageContainer,
                                      ImpressionsTaskFactory splitTaskFactory,
                                      TelemetryRuntimeProducer telemetryStorage,
                                      int impressionsQueueSize,
                                      long impressionsChunkSize,
                                      int impressionsRefreshRate,
                                      int impressionsCounterRefreshRate,
                                      int uniqueKeysRefreshRate,
                                      boolean userConsentIsGranted) {
        mSplitTaskExecutor = splitTaskExecutor;
        mStorageContainer = storageContainer;
        mSplitTaskFactory = splitTaskFactory;
        mTelemetryStorage = telemetryStorage;
        mImpressionsQueueSize = impressionsQueueSize;
        mImpressionsChunkSize = impressionsChunkSize;
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mUniqueKeysRefreshRate = uniqueKeysRefreshRate;
        mUserConsentIsGranted = userConsentIsGranted;
    }

    public ProcessStrategy getStrategy(ImpressionsMode mode) {
        ImpressionManagerRetryTimerProviderImpl impressionManagerRetryTimerProvider = new ImpressionManagerRetryTimerProviderImpl(mSplitTaskExecutor);
        switch (mode) {
            case DEBUG:
                return new DebugStrategy(
                        new RecorderSyncHelperImpl<>(
                                SplitTaskType.IMPRESSIONS_RECORDER,
                                mStorageContainer.getImpressionsStorage(),
                                mImpressionsQueueSize,
                                mImpressionsChunkSize,
                                mSplitTaskExecutor),
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        mImpressionsRefreshRate
                );
            case NONE:
                return new NoneStrategy(
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        new ImpressionsCounter(),
                        new UniqueKeysTrackerImpl(),
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        impressionManagerRetryTimerProvider.getUniqueKeysTimer(),
                        mImpressionsCounterRefreshRate,
                        mUniqueKeysRefreshRate,
                        mUserConsentIsGranted
                );
            default:
                return new OptimizedStrategy(
                        new ImpressionsObserverImpl(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE),
                        new ImpressionsCounter(),
                        new RecorderSyncHelperImpl<>(
                                SplitTaskType.IMPRESSIONS_RECORDER,
                                mStorageContainer.getImpressionsStorage(),
                                mImpressionsQueueSize,
                                mImpressionsChunkSize,
                                mSplitTaskExecutor),
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        mImpressionsRefreshRate,
                        mImpressionsCounterRefreshRate,
                        mUserConsentIsGranted
                );
        }
    }
}
