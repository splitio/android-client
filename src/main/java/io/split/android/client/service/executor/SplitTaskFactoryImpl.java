package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.TestingConfig;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.service.impressions.SaveImpressionsCountTask;
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTaskConfig;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitInPlaceUpdateTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.telemetry.TelemetryConfigRecorderTask;
import io.split.android.client.service.telemetry.TelemetryStatsRecorderTask;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;
import io.split.android.client.service.telemetry.TelemetryTaskFactoryImpl;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class SplitTaskFactoryImpl implements SplitTaskFactory {

    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final String mSplitsFilterQueryStringFromConfig;
    private final ISplitEventsManager mEventsManager;
    private final TelemetryTaskFactory mTelemetryTaskFactory;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final List<SplitFilter> mFilters;

    @SuppressLint("VisibleForTests")
    public SplitTaskFactoryImpl(@NonNull SplitClientConfig splitClientConfig,
                                @NonNull SplitApiFacade splitApiFacade,
                                @NonNull SplitStorageContainer splitStorageContainer,
                                @Nullable String splitsFilterQueryString,
                                ISplitEventsManager eventsManager,
                                @Nullable List<SplitFilter> filters,
                                @NonNull Set<String> configuredFlagSets,
                                @Nullable TestingConfig testingConfig) {

        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitsFilterQueryStringFromConfig = splitsFilterQueryString;
        mEventsManager = eventsManager;
        mSplitChangeProcessor = new SplitChangeProcessor(configuredFlagSets);

        TelemetryStorage telemetryStorage = mSplitsStorageContainer.getTelemetryStorage();
        mTelemetryRuntimeProducer = telemetryStorage;
        if (testingConfig != null) {
            mSplitsSyncHelper = new SplitsSyncHelper(mSplitApiFacade.getSplitFetcher(),
                    mSplitsStorageContainer.getSplitsStorage(),
                    mSplitChangeProcessor,
                    mTelemetryRuntimeProducer,
                    new ReconnectBackoffCounter(1, testingConfig.getCdnBackoffTime()));
        } else {
            mSplitsSyncHelper = new SplitsSyncHelper(mSplitApiFacade.getSplitFetcher(),
                    mSplitsStorageContainer.getSplitsStorage(),
                    mSplitChangeProcessor,
                    mTelemetryRuntimeProducer);
        }

        mTelemetryTaskFactory = new TelemetryTaskFactoryImpl(mSplitApiFacade.getTelemetryConfigRecorder(),
                mSplitApiFacade.getTelemetryStatsRecorder(),
                telemetryStorage,
                splitClientConfig,
                mSplitsStorageContainer.getSplitsStorage(),
                mSplitsStorageContainer.getMySegmentsStorageContainer());

        mFilters = (filters == null) ? new ArrayList<>() : filters;
    }

    @Override
    public EventsRecorderTask createEventsRecorderTask() {
        return new EventsRecorderTask(
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getPersistentEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()),
                mSplitsStorageContainer.getTelemetryStorage());
    }

    @Override
    public ImpressionsRecorderTask createImpressionsRecorderTask() {
        return new ImpressionsRecorderTask(
                mSplitApiFacade.getImpressionsRecorder(),
                mSplitsStorageContainer.getPersistentImpressionsStorage(),
                new ImpressionsRecorderTaskConfig(
                        mSplitClientConfig.impressionsPerPush(),
                        ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES,
                        mSplitClientConfig.shouldRecordTelemetry()),
                mSplitsStorageContainer.getTelemetryStorage());
    }

    @Override
    public SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration) {
        return SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), checkCacheExpiration,
                mSplitClientConfig.cacheExpirationInSeconds(), mSplitsFilterQueryStringFromConfig, mEventsManager, mSplitsStorageContainer.getTelemetryStorage());
    }

    @Override
    public LoadSplitsTask createLoadSplitsTask(String splitsFilterQueryStringFromConfig) {
        return new LoadSplitsTask(mSplitsStorageContainer.getSplitsStorage(), splitsFilterQueryStringFromConfig);
    }

    @Override
    public SplitKillTask createSplitKillTask(Split split) {
        return new SplitKillTask(mSplitsStorageContainer.getSplitsStorage(), split, mEventsManager);
    }

    @Override
    public SplitsUpdateTask createSplitsUpdateTask(long since) {
        return new SplitsUpdateTask(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), since, mEventsManager);
    }

    @Override
    public FilterSplitsInCacheTask createFilterSplitsInCacheTask() {
        return new FilterSplitsInCacheTask(mSplitsStorageContainer.getPersistentSplitsStorage(),
                mFilters, mSplitsFilterQueryStringFromConfig);
    }

    @Override
    public CleanUpDatabaseTask createCleanUpDatabaseTask(long maxTimestamp) {
        return new CleanUpDatabaseTask(mSplitsStorageContainer.getPersistentEventsStorage(),
                mSplitsStorageContainer.getPersistentImpressionsStorage(),
                mSplitsStorageContainer.getImpressionsCountStorage(),
                mSplitsStorageContainer.getPersistentImpressionsUniqueStorage(),
                maxTimestamp);
    }

    @Override
    public SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> counts) {
        return new SaveImpressionsCountTask(mSplitsStorageContainer.getImpressionsCountStorage(), counts);
    }

    @Override
    public ImpressionsCountRecorderTask createImpressionsCountRecorderTask() {
        return new ImpressionsCountRecorderTask(
                mSplitApiFacade.getImpressionsCountRecorder(),
                mSplitsStorageContainer.getImpressionsCountStorage(),
                mSplitsStorageContainer.getTelemetryStorage());
    }

    @Override
    public SaveUniqueImpressionsTask createSaveUniqueImpressionsTask(Map<String, Set<String>> uniqueImpressions) {
        return new SaveUniqueImpressionsTask(mSplitsStorageContainer.getPersistentImpressionsUniqueStorage(), uniqueImpressions);
    }

    @Override
    public UniqueKeysRecorderTask createUniqueImpressionsRecorderTask() {
        return new UniqueKeysRecorderTask(
                mSplitApiFacade.getUniqueKeysRecorder(), mSplitsStorageContainer.getPersistentImpressionsUniqueStorage(),
                new UniqueKeysRecorderTaskConfig(
                        mSplitClientConfig.mtkPerPush(),
                        ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES)
        );
    }

    @Override
    public TelemetryConfigRecorderTask getTelemetryConfigRecorderTask() {
        return mTelemetryTaskFactory.getTelemetryConfigRecorderTask();
    }

    @Override
    public TelemetryStatsRecorderTask getTelemetryStatsRecorderTask() {
        return mTelemetryTaskFactory.getTelemetryStatsRecorderTask();
    }

    @Override
    public SplitInPlaceUpdateTask createSplitsUpdateTask(Split featureFlag, long since) {
        return new SplitInPlaceUpdateTask(mSplitsStorageContainer.getSplitsStorage(), mSplitChangeProcessor, mEventsManager, mTelemetryRuntimeProducer, featureFlag, since);
    }
}
