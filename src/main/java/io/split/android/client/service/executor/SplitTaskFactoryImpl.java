package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.FilterGrouper;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.attributes.LoadAttributesTask;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.service.impressions.SaveImpressionsCountTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.telemetry.TelemetryConfigRecorderTask;
import io.split.android.client.service.telemetry.TelemetryStatsRecorderTask;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;
import io.split.android.client.service.telemetry.TelemetryTaskFactoryImpl;
import io.split.android.client.storage.SplitStorageContainer;

public class SplitTaskFactoryImpl implements SplitTaskFactory {

    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitsSyncHelper mSplitsSyncHelper;
    private final String mSplitsFilterQueryString;
    private final SplitEventsManager mEventsManager;
    private final TelemetryTaskFactory mTelemetryTaskFactory;

    public SplitTaskFactoryImpl(@NonNull SplitClientConfig splitClientConfig,
                                @NonNull SplitApiFacade splitApiFacade,
                                @NonNull SplitStorageContainer splitStorageContainer,
                                @Nullable String splistFilterQueryString,
                                SplitEventsManager eventsManager) {

        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mSplitsFilterQueryString = splistFilterQueryString;
        mEventsManager = eventsManager;
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitApiFacade.getSplitFetcher(),
                mSplitsStorageContainer.getSplitsStorage(),
                new SplitChangeProcessor());
        mTelemetryTaskFactory = new TelemetryTaskFactoryImpl(mSplitApiFacade.getTelemetryConfigRecorder(),
                mSplitsStorageContainer.getTelemetryConsumer(),
                splitClientConfig);

    }

    @Override
    public EventsRecorderTask createEventsRecorderTask() {
        return new EventsRecorderTask(
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()));
    }

    @Override
    public ImpressionsRecorderTask createImpressionsRecorderTask() {
        return new ImpressionsRecorderTask(
                mSplitApiFacade.getImpressionsRecorder(),
                mSplitsStorageContainer.getImpressionsStorage(),
                new ImpressionsRecorderTaskConfig(
                        mSplitClientConfig.impressionsPerPush(),
                        ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES));
    }

    @Override
    public SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration) {

        return new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), checkCacheExpiration,
                mSplitClientConfig.cacheExpirationInSeconds(), mSplitsFilterQueryString, mEventsManager);
    }

    @Override
    public MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache) {
        return new MySegmentsSyncTask(
                mSplitApiFacade.getMySegmentsFetcher(),
                mSplitsStorageContainer.getMySegmentsStorage(),
                avoidCache, mEventsManager);
    }

    @Override
    public LoadMySegmentsTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public LoadSplitsTask createLoadSplitsTask() {
        return new LoadSplitsTask(mSplitsStorageContainer.getSplitsStorage());
    }

    @Override
    public SplitKillTask createSplitKillTask(Split split) {
        return new SplitKillTask(mSplitsStorageContainer.getSplitsStorage(), split, mEventsManager);
    }

    @Override
    public MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments) {
        return new MySegmentsOverwriteTask(mSplitsStorageContainer.getMySegmentsStorage(), segments, mEventsManager);
    }

    @Override
    public MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, String segmentName) {
        return new MySegmentsUpdateTask(
                mSplitsStorageContainer.getMySegmentsStorage(), add,
                segmentName, mEventsManager);
    }

    @Override
    public SplitsUpdateTask createSplitsUpdateTask(long since) {
        return new SplitsUpdateTask(mSplitsSyncHelper, mSplitsStorageContainer.getSplitsStorage(), since, mEventsManager);
    }

    @Override
    public FilterSplitsInCacheTask createFilterSplitsInCacheTask() {
        List<SplitFilter> filters = new FilterGrouper().group(mSplitClientConfig.syncConfig().getFilters());
        return new FilterSplitsInCacheTask(mSplitsStorageContainer.getPersistentSplitsStorage(),
                filters, mSplitsFilterQueryString);
    }
    @Override
    public CleanUpDatabaseTask createCleanUpDatabaseTask(long maxTimestamp) {
        return new CleanUpDatabaseTask(mSplitsStorageContainer.getEventsStorage(),
                mSplitsStorageContainer.getImpressionsStorage(), maxTimestamp);
    }

    @Override
    public SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> counts) {
        return new SaveImpressionsCountTask(mSplitsStorageContainer.getImpressionsCountStorage() ,counts);
    }

    @Override
    public ImpressionsCountRecorderTask createImpressionsCountRecorderTask() {
        return new ImpressionsCountRecorderTask(
                mSplitApiFacade.getImpressionsCountRecorder(),
                mSplitsStorageContainer.getImpressionsCountStorage());
    }

    @Override
    public LoadAttributesTask createLoadAttributesTask(boolean persistentAttributesEnabled) {
        return new LoadAttributesTask(mSplitsStorageContainer.getAttributesStorage(),
                (persistentAttributesEnabled) ? mSplitsStorageContainer.getPersistentAttributesStorage() : null);
    }

    @Override
    public TelemetryConfigRecorderTask getTelemetryConfigRecorderTask() {
        return mTelemetryTaskFactory.getTelemetryConfigRecorderTask();
    }

    @Override
    public TelemetryStatsRecorderTask getTelemetryStatsRecorderTask() {
        return mTelemetryTaskFactory.getTelemetryStatsRecorderTask();
    }
}
