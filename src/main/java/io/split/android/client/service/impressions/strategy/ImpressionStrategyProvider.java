package io.split.android.client.service.impressions.strategy;

import android.util.Pair;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManagerRetryTimerProviderImpl;
import io.split.android.client.service.impressions.ImpressionsCounter;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsObserver;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
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
    private final long nImpressionsChunkSize;
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
        nImpressionsChunkSize = impressionsChunkSize;
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsCounterRefreshRate = impressionsCounterRefreshRate;
        mUniqueKeysRefreshRate = uniqueKeysRefreshRate;
        mUserConsentIsGranted = userConsentIsGranted;
    }

    public Pair<ProcessStrategy, PeriodicTracker> getStrategy(ImpressionsMode mode) {
        ImpressionManagerRetryTimerProviderImpl impressionManagerRetryTimerProvider = new ImpressionManagerRetryTimerProviderImpl(mSplitTaskExecutor);
        ProcessStrategy strategy;
        PeriodicTracker tracker;
        RecorderSyncHelperImpl<KeyImpression> impressionsSyncHelper;
        switch (mode) {
            case DEBUG:
                impressionsSyncHelper = buildImpressionSyncHelper();
                strategy = new DebugStrategy(
                        new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE),
                        impressionsSyncHelper,
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        mImpressionsRefreshRate);
                tracker = new DebugTracker(
                        impressionsSyncHelper,
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        mImpressionsRefreshRate);
                break;
            case NONE:
                ImpressionsCounter impressionsCounter = new ImpressionsCounter();
                UniqueKeysTrackerImpl uniqueKeysTracker = new UniqueKeysTrackerImpl();
                strategy = new NoneStrategy(
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        impressionsCounter,
                        uniqueKeysTracker,
                        mUserConsentIsGranted);
                tracker = new NoneTracker(
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        impressionsCounter,
                        uniqueKeysTracker,
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        impressionManagerRetryTimerProvider.getUniqueKeysTimer(),
                        mImpressionsCounterRefreshRate,
                        mUniqueKeysRefreshRate,
                        mUserConsentIsGranted);
                break;
            default:
                impressionsCounter = new ImpressionsCounter();
                impressionsSyncHelper = buildImpressionSyncHelper();
                strategy = new OptimizedStrategy(
                        new ImpressionsObserver(ServiceConstants.LAST_SEEN_IMPRESSION_CACHE_SIZE),
                        impressionsCounter,
                        impressionsSyncHelper,
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        mTelemetryStorage);
                tracker = new OptimizedTracker(impressionsCounter,
                        impressionsSyncHelper,
                        mSplitTaskExecutor,
                        mSplitTaskFactory,
                        impressionManagerRetryTimerProvider.getImpressionsTimer(),
                        impressionManagerRetryTimerProvider.getImpressionsCountTimer(),
                        mImpressionsRefreshRate, mImpressionsCounterRefreshRate, mUserConsentIsGranted);
                break;
        }

        return Pair.create(strategy, tracker);
    }

    @NonNull
    private RecorderSyncHelperImpl<KeyImpression> buildImpressionSyncHelper() {
        return new RecorderSyncHelperImpl<>(
                SplitTaskType.IMPRESSIONS_RECORDER,
                mStorageContainer.getImpressionsStorage(),
                mImpressionsQueueSize,
                nImpressionsChunkSize,
                mSplitTaskExecutor);
    }
}
