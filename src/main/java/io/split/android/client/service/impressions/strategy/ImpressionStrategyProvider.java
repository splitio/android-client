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
    private final ImpressionStrategyConfig mImpressionStrategyConfig;

    public ImpressionStrategyProvider(SplitTaskExecutor splitTaskExecutor,
                                      SplitStorageContainer storageContainer,
                                      ImpressionsTaskFactory splitTaskFactory,
                                      TelemetryRuntimeProducer telemetryStorage,
                                      ImpressionStrategyConfig config) {
        mSplitTaskExecutor = splitTaskExecutor;
        mStorageContainer = storageContainer;
        mSplitTaskFactory = splitTaskFactory;
        mTelemetryStorage = telemetryStorage;
        mImpressionStrategyConfig = config;
    }

    public ProcessStrategy getStrategy(ImpressionsMode mode) {
        ImpressionManagerRetryTimerProviderImpl impressionManagerRetryTimerProvider = new ImpressionManagerRetryTimerProviderImpl(mSplitTaskExecutor);
        switch (mode) {
            case DEBUG:
                return new DebugStrategy(
                        new ImpressionsObserverImpl(mStorageContainer.getImpressionsObserverCachePersistentStorage(), ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE),
                        new RecorderSyncHelperImpl<>(
                                SplitTaskType.IMPRESSIONS_RECORDER,
                                mStorageContainer.getImpressionsStorage(),
                                mImpressionStrategyConfig.getImpressionsQueueSize(),
                                mImpressionStrategyConfig.getImpressionsChunkSize(),
                                mSplitTaskExecutor),
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        mImpressionStrategyConfig.getImpressionsRefreshRate()
                );
            case NONE:
                return new NoneStrategy(
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        new ImpressionsCounter(mImpressionStrategyConfig.getDedupeTimeIntervalInMs()),
                        new UniqueKeysTrackerImpl(),
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        impressionManagerRetryTimerProvider.getUniqueKeysTimer(),
                        mImpressionStrategyConfig.getImpressionsCounterRefreshRate(),
                        mImpressionStrategyConfig.getUniqueKeysRefreshRate(),
                        mImpressionStrategyConfig.isUserConsentGranted()
                );
            default:
                return new OptimizedStrategy(
                        new ImpressionsObserverImpl(mStorageContainer.getImpressionsObserverCachePersistentStorage(), ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE),
                        new ImpressionsCounter(mImpressionStrategyConfig.getDedupeTimeIntervalInMs()),
                        new RecorderSyncHelperImpl<>(
                                SplitTaskType.IMPRESSIONS_RECORDER,
                                mStorageContainer.getImpressionsStorage(),
                                mImpressionStrategyConfig.getImpressionsQueueSize(),
                                mImpressionStrategyConfig.getImpressionsChunkSize(),
                                mSplitTaskExecutor),
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        mImpressionStrategyConfig.getImpressionsRefreshRate(),
                        mImpressionStrategyConfig.getImpressionsCounterRefreshRate(),
                        mImpressionStrategyConfig.isUserConsentGranted(),
                        mImpressionStrategyConfig.getDedupeTimeIntervalInMs()
                );
        }
    }
}
