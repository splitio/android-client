package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.events.EventsRecorderTaskConfig;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTaskConfig;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.sseauthentication.SseAuthenticationTask;
import io.split.android.client.service.sseclient.SseChannelsParser;
import io.split.android.client.storage.SplitStorageContainer;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskFactoryImpl implements SplitTaskFactory {

    private final SplitApiFacade mSplitApiFacade;
    private final SplitStorageContainer mSplitsStorageContainer;
    private final SplitClientConfig mSplitClientConfig;
    private final String mApiKey;
    private final String mUserKey;

    public SplitTaskFactoryImpl(@NonNull SplitClientConfig splitClientConfig,
                                @NonNull SplitApiFacade splitApiFacade,
                                @NonNull SplitStorageContainer splitStorageContainer,
                                @NonNull String apiKey,
                                @NonNull String userKey) {

        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitApiFacade = checkNotNull(splitApiFacade);
        mSplitsStorageContainer = checkNotNull(splitStorageContainer);
        mApiKey = checkNotNull(apiKey);
        mUserKey = checkNotNull(userKey);
    }

    @Override
    public SplitTask createEventsRecorderTask() {
        return new EventsRecorderTask(
                mSplitApiFacade.getEventsRecorder(),
                mSplitsStorageContainer.getEventsStorage(),
                new EventsRecorderTaskConfig(mSplitClientConfig.eventsPerPush()));
    }

    @Override
    public SplitTask createImpressionsRecorderTask() {
        return new ImpressionsRecorderTask(
                mSplitApiFacade.getImpressionsRecorder(),
                mSplitsStorageContainer.getImpressionsStorage(),
                new ImpressionsRecorderTaskConfig(
                        mSplitClientConfig.impressionsPerPush(),
                        ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES));
    }

    @Override
    public SplitTask createSplitsSyncTask() {
        return new SplitsSyncTask(
                mSplitApiFacade.getSplitFetcher(),
                mSplitsStorageContainer.getSplitsStorage(),
                new SplitChangeProcessor());
    }

    @Override
    public SplitTask createMySegmentsSyncTask() {
        return new MySegmentsSyncTask(
                mSplitApiFacade.getMySegmentsFetcher(),
                mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public SplitTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public SplitTask createLoadSplitsTask() {
        return new LoadSplitsTask(mSplitsStorageContainer.getSplitsStorage());
    }

    @Override
    public SplitTask createSseAuthenticationTask() {
        return new SseAuthenticationTask(mSplitApiFacade.getSseAuthenticationFetcher(),
                mUserKey, new SseChannelsParser());
    }

    @Override
    public SplitTask createSplitKillTask() {
        return new SplitKillTask(mSplitsStorageContainer.getSplitsStorage());
    }

    @Override
    public SplitTask createMySegmentsUpdateTask() {
        return new MySegmentsUpdateTask(mSplitsStorageContainer.getMySegmentsStorage());
    }

    @Override
    public SplitTask createSplitsUpdateTask() {
        return new SplitsUpdateTask(
                mSplitApiFacade.getSplitFetcher(),
                mSplitsStorageContainer.getSplitsStorage(),
                new SplitChangeProcessor());
    }
}
