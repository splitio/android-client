package io.split.android.client.service.synchronizer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.shared.UserConsent;

public class FeatureFlagsSynchronizerImpl implements FeatureFlagsSynchronizer {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskExecutor mSplitsTaskExecutor;
    private final SplitClientConfig mSplitClientConfig;
    private final ISplitEventsManager mSplitEventsManager;
    private final SplitTaskFactory mSplitTaskFactory;

    private LoadLocalDataListener mLoadLocalSplitsListener;

    private String mSplitsFetcherTaskId;
    private final RetryBackoffCounterTimer mSplitsSyncRetryTimer;
    private final RetryBackoffCounterTimer mSplitsUpdateRetryTimer;

    public FeatureFlagsSynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                                        @NonNull SplitTaskExecutor taskExecutor,
                                        @NonNull SplitTaskExecutor splitSingleThreadTaskExecutor,
                                        @NonNull SplitTaskFactory splitTaskFactory,
                                        @NonNull ISplitEventsManager splitEventsManager,
                                        @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsTaskExecutor = splitSingleThreadTaskExecutor;
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitEventsManager = checkNotNull(splitEventsManager);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mSplitsSyncRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);
        mSplitsUpdateRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);

        setupListeners();
        mSplitsSyncRetryTimer.setTask(mSplitTaskFactory.createSplitsSyncTask(true), null);
    }

    @Override
    public void loadSplitsFromCache() {
        submitSplitLoadingTask(mLoadLocalSplitsListener);
    }

    @Override
    public void loadAndSynchronizeSplits() {
        List<SplitTaskBatchItem> enqueued = new ArrayList<>();
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createFilterSplitsInCacheTask(), null));
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createLoadSplitsTask(), mLoadLocalSplitsListener));
        enqueued.add(new SplitTaskBatchItem(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                synchronizeSplits();
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        }, null));
        mTaskExecutor.executeSerially(enqueued);
    }

    @Override
    public void synchronizeSplits(long since) {
        mSplitsUpdateRetryTimer.setTask(mSplitTaskFactory.createSplitsUpdateTask(since), null);
        mSplitsUpdateRetryTimer.start();
    }

    @Override
    public void synchronizeSplits() {
        mSplitsSyncRetryTimer.start();
    }

    @Override
    public void startFeatureFlagsPeriodicFetching() {
        scheduleSplitsFetcherTask();
    }

    @Override
    public void stopFeatureFlagsPeriodicFetching() {
        mSplitsTaskExecutor.stopTask(mSplitsFetcherTaskId);
    }

    private void setupListeners() {
        mLoadLocalSplitsListener = new LoadLocalDataListener(
                mSplitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    public void pause() {
        stopFeatureFlagsPeriodicFetching();
        mTaskExecutor.pause();
        mSplitsTaskExecutor.pause();
    }

    public void resume() {
        mSplitsTaskExecutor.resume();
        if (mSplitClientConfig.userConsent() == UserConsent.GRANTED) {
            startFeatureFlagsPeriodicFetching();
        }
    }

    @Override
    public void stopSynchronization() {
        mSplitsSyncRetryTimer.stop();
        mSplitsUpdateRetryTimer.stop();
    }

    private void scheduleSplitsFetcherTask() {
        mSplitsFetcherTaskId = mSplitsTaskExecutor.schedule(
                mSplitTaskFactory.createSplitsSyncTask(false),
                mSplitClientConfig.featuresRefreshRate(),
                mSplitClientConfig.featuresRefreshRate(),
                null);
    }

    @Override
    public void submitSplitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
    }
}
