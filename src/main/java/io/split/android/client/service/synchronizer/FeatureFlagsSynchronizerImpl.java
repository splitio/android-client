package io.split.android.client.service.synchronizer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

public class FeatureFlagsSynchronizerImpl implements FeatureFlagsSynchronizer {

    private final SplitTaskExecutor mTaskExecutor;
    private final SplitTaskExecutor mSplitsTaskExecutor;
    private final SplitClientConfig mSplitClientConfig;
    private final SplitTaskFactory mSplitTaskFactory;

    private final LoadLocalDataListener mLoadLocalSplitsListener;

    private String mSplitsFetcherTaskId;
    private final RetryBackoffCounterTimer mSplitsSyncRetryTimer;
    private final RetryBackoffCounterTimer mSplitsUpdateRetryTimer;
    @Nullable
    private final SplitTaskExecutionListener mSplitsSyncListener;
    private final AtomicBoolean mIsSynchronizing = new AtomicBoolean(true);

    public FeatureFlagsSynchronizerImpl(@NonNull SplitClientConfig splitClientConfig,
                                        @NonNull SplitTaskExecutor taskExecutor,
                                        @NonNull SplitTaskExecutor splitSingleThreadTaskExecutor,
                                        @NonNull SplitTaskFactory splitTaskFactory,
                                        @NonNull ISplitEventsManager splitEventsManager,
                                        @NonNull RetryBackoffCounterTimerFactory retryBackoffCounterTimerFactory,
                                        @Nullable PushManagerEventBroadcaster pushManagerEventBroadcaster) {

        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitsTaskExecutor = splitSingleThreadTaskExecutor;
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mSplitTaskFactory = checkNotNull(splitTaskFactory);
        mSplitsSyncRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);
        mSplitsUpdateRetryTimer = retryBackoffCounterTimerFactory.create(mSplitsTaskExecutor, 1);

        // pushManagerEventBroadcaster could be null when in single sync mode
        if (pushManagerEventBroadcaster != null) {
            mSplitsSyncListener = new SplitTaskExecutionListener() {
                @Override
                public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                    if (taskInfo.getStatus() == SplitTaskExecutionStatus.SUCCESS) {
                        pushManagerEventBroadcaster.pushMessage(new PushStatusEvent(PushStatusEvent.EventType.SUCCESSFUL_SYNC));
                    } else {
                        if (Boolean.TRUE.equals(taskInfo.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY))) {
                            stopPeriodicFetching();
                            mIsSynchronizing.compareAndSet(true, false);
                        }
                    }
                }
            };
        } else {
            mSplitsSyncListener = null;
        }

        mSplitsSyncRetryTimer.setTask(mSplitTaskFactory.createSplitsSyncTask(true), mSplitsSyncListener);
        mLoadLocalSplitsListener = new LoadLocalDataListener(
                splitEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    @Override
    public void loadAndSynchronize() {
        List<SplitTaskBatchItem> enqueued = new ArrayList<>();
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createFilterSplitsInCacheTask(), null));
        enqueued.add(new SplitTaskBatchItem(mSplitTaskFactory.createLoadSplitsTask(), mLoadLocalSplitsListener));
        enqueued.add(new SplitTaskBatchItem(() -> {
            synchronize();
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        }, null));
        mTaskExecutor.executeSerially(enqueued);
    }

    @Override
    public void synchronize(long since) {
        mSplitsUpdateRetryTimer.setTask(mSplitTaskFactory.createSplitsUpdateTask(since), mSplitsSyncListener);
        mSplitsUpdateRetryTimer.start();
    }

    @Override
    public void synchronize() {
        mSplitsSyncRetryTimer.start();
    }

    @Override
    public void startPeriodicFetching() {
        if (mIsSynchronizing.get()) {
            scheduleSplitsFetcherTask();
        }
    }

    @Override
    public void stopPeriodicFetching() {
        mSplitsTaskExecutor.stopTask(mSplitsFetcherTaskId);
    }

    @Override
    public void stopSynchronization() {
        mSplitsSyncRetryTimer.stop();
        mSplitsUpdateRetryTimer.stop();
    }

    @Override
    public void submitLoadingTask(SplitTaskExecutionListener listener) {
        mTaskExecutor.submit(mSplitTaskFactory.createLoadSplitsTask(),
                listener);
    }

    private void scheduleSplitsFetcherTask() {
        if (mSplitsFetcherTaskId != null) {
            stopPeriodicFetching();
        }

        mSplitsFetcherTaskId = mSplitsTaskExecutor.schedule(
                mSplitTaskFactory.createSplitsSyncTask(false),
                mSplitClientConfig.featuresRefreshRate(),
                mSplitClientConfig.featuresRefreshRate(),
                mSplitsSyncListener);
    }
}
