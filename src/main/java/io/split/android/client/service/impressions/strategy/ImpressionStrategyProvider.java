package io.split.android.client.service.impressions.strategy;

import androidx.core.util.Pair;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManagerRetryTimerProviderImpl;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.impressions.observer.ImpressionsObserverImpl;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
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
    private final ImpressionsCounter mImpressionsCounter;
    private final ImpressionManagerRetryTimerProviderImpl mImpressionManagerRetryTimerProvider;

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
        mImpressionsCounter = new ImpressionsCounter(mImpressionStrategyConfig.getDedupeTimeIntervalInMs());
        mImpressionManagerRetryTimerProvider = new ImpressionManagerRetryTimerProviderImpl(mSplitTaskExecutor);
    }

    public Pair<ProcessStrategy, PeriodicTracker> getStrategy(ImpressionsMode mode) {
        ImpressionsObserverImpl impressionsObserver = new ImpressionsObserverImpl(mStorageContainer.getImpressionsObserverCachePersistentStorage(), ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE);
        RecorderSyncHelperImpl<KeyImpression> impressionsSyncHelper = new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mStorageContainer.getImpressionsStorage(),
                mImpressionStrategyConfig.getImpressionsQueueSize(),
                mImpressionStrategyConfig.getImpressionsChunkSize(),
                mSplitTaskExecutor);
        if (mode == ImpressionsMode.DEBUG) {
            DebugTracker tracker = new DebugTracker(
                    impressionsObserver,
                    impressionsSyncHelper,
                    mSplitTaskExecutor,
                    mSplitTaskFactory,
                    mImpressionManagerRetryTimerProvider.getImpressionsTimer(),
                    mImpressionStrategyConfig.getImpressionsRefreshRate());
            DebugStrategy debugStrategy = new DebugStrategy(
                    impressionsObserver,
                    impressionsSyncHelper,
                    mSplitTaskExecutor,
                    mSplitTaskFactory,
                    mTelemetryStorage);
            return new Pair<>(debugStrategy, tracker);
        } else if (mode == ImpressionsMode.NONE) {
            return new Pair<>(null, null);
        } else {
            OptimizedStrategy optimizedStrategy = new OptimizedStrategy(
                    impressionsObserver,
                    mImpressionsCounter,
                    impressionsSyncHelper,
                    mSplitTaskExecutor,
                    mSplitTaskFactory,
                    mTelemetryStorage,
                    mImpressionStrategyConfig.getDedupeTimeIntervalInMs());
            OptimizedTracker optimizedTracker = new OptimizedTracker(
                    impressionsObserver,
                    impressionsSyncHelper,
                    mSplitTaskExecutor,
                    mSplitTaskFactory,
                    mImpressionManagerRetryTimerProvider.getImpressionsTimer(),
                    mImpressionStrategyConfig.getImpressionsRefreshRate(),
                    mImpressionStrategyConfig.isUserConsentGranted()
            );
            return new Pair<>(optimizedStrategy, optimizedTracker);
        }
    }

    public Pair<ProcessStrategy, PeriodicTracker> getNoneComponents() {
        UniqueKeysTracker uniqueKeysTracker = new UniqueKeysTrackerImpl();
        NoneStrategy noneStrategy = new NoneStrategy(
                mSplitTaskExecutor,
                mSplitTaskFactory,
                mImpressionsCounter,
                uniqueKeysTracker,
                mImpressionStrategyConfig.isUserConsentGranted());
        NoneTracker noneTracker = new NoneTracker(
                mSplitTaskExecutor,
                mSplitTaskFactory,
                mImpressionsCounter,
                uniqueKeysTracker,
                mImpressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                mImpressionManagerRetryTimerProvider.getUniqueKeysTimer(),
                mImpressionStrategyConfig.getImpressionsCounterRefreshRate(),
                mImpressionStrategyConfig.getUniqueKeysRefreshRate(),
                mImpressionStrategyConfig.isUserConsentGranted());
        return new Pair<>(noneStrategy, noneTracker);
    }
}
