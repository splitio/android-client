package io.split.android.client.service.synchronizer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Objects;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

public class FeatureFlagsSynchronizerImplTest {

    private SplitClientConfig mConfig;
    private SplitTaskExecutor mTaskExecutor;
    private SplitTaskExecutor mSingleThreadTaskExecutor;
    private SplitTaskFactory mTaskFactory;
    private ISplitEventsManager mEventsManager;
    private RetryBackoffCounterTimerFactory mRetryBackoffCounterFactory;
    private RetryBackoffCounterTimer mRetryTimerSplitsUpdate;
    private RetryBackoffCounterTimer mRetryTimerSplitsSync;
    private PushManagerEventBroadcaster mPushManagerEventBroadcaster;

    private FeatureFlagsSynchronizerImpl mFeatureFlagsSynchronizer;

    @Before
    public void setUp() {
        mConfig = mock(SplitClientConfig.class);
        mTaskExecutor = mock(SplitTaskExecutor.class);
        mSingleThreadTaskExecutor = mock(SplitTaskExecutor.class);
        mTaskFactory = mock(SplitTaskFactory.class);
        mEventsManager = mock(ISplitEventsManager.class);
        mRetryBackoffCounterFactory = mock(RetryBackoffCounterTimerFactory.class);
        mRetryTimerSplitsUpdate = mock(RetryBackoffCounterTimer.class);
        mRetryTimerSplitsSync = mock(RetryBackoffCounterTimer.class);
        mPushManagerEventBroadcaster = mock(PushManagerEventBroadcaster.class);
        when(mRetryBackoffCounterFactory.create(mSingleThreadTaskExecutor, 1))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mFeatureFlagsSynchronizer = new FeatureFlagsSynchronizerImpl(mConfig,
                mTaskExecutor, mSingleThreadTaskExecutor, mTaskFactory,
                mEventsManager, mRetryBackoffCounterFactory, mPushManagerEventBroadcaster);
    }

    @Test
    public void synchronizeSplitsWithSince() {
        SplitsUpdateTask task = mock(SplitsUpdateTask.class);
        when(mTaskFactory.createSplitsUpdateTask(1000)).thenReturn(task);

        mFeatureFlagsSynchronizer.synchronize(1000);

        verify(mRetryTimerSplitsUpdate).setTask(task, null);
        verify(mRetryTimerSplitsUpdate).start();
    }

    @Test
    public void loadLocalData() {
        LoadSplitsTask mockTask = mock(LoadSplitsTask.class);
        when(mockTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        when(mTaskFactory.createLoadSplitsTask()).thenReturn(mockTask);
        when(mRetryBackoffCounterFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mFeatureFlagsSynchronizer.loadFromCache();

        verify(mTaskExecutor).submit(eq(mockTask), argThat(Objects::nonNull));
    }

    @Test
    public void loadAndSynchronizeSplits() {
        LoadSplitsTask mockLoadTask = mock(LoadSplitsTask.class);
        when(mockLoadTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        when(mTaskFactory.createLoadSplitsTask()).thenReturn(mockLoadTask);

        FilterSplitsInCacheTask mockFilterTask = mock(FilterSplitsInCacheTask.class);
        when(mockFilterTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.FILTER_SPLITS_CACHE));
        when(mTaskFactory.createFilterSplitsInCacheTask()).thenReturn(mockFilterTask);

        SplitsSyncTask mockSplitSyncTask = mock(SplitsSyncTask.class);
        when(mockSplitSyncTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));
        when(mTaskFactory.createSplitsSyncTask(true)).thenReturn(mockSplitSyncTask);

        when(mRetryBackoffCounterFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mFeatureFlagsSynchronizer.loadAndSynchronize();

        verify(mTaskFactory).createFilterSplitsInCacheTask();
        verify(mTaskFactory).createLoadSplitsTask();

        ArgumentCaptor<List<SplitTaskBatchItem>> argument = ArgumentCaptor.forClass(List.class);
        verify(mTaskExecutor).executeSerially(argument.capture());
        assertEquals(3, argument.getValue().size());
    }

    @Test
    public void splitExecutorSchedule() {
        SplitsSyncTask mockTask = mock(SplitsSyncTask.class);
        when(mTaskFactory.createSplitsSyncTask(false)).thenReturn(mockTask);
        mFeatureFlagsSynchronizer.startPeriodicFetching();
        verify(mSingleThreadTaskExecutor).schedule(
                eq(mockTask), anyLong(), anyLong(),
                any());
    }

    @Test
    public void stopSynchronization() {

        mFeatureFlagsSynchronizer.stopSynchronization();

        verify(mRetryTimerSplitsSync).stop();
        verify(mRetryTimerSplitsUpdate).stop();
    }

    @Test
    public void synchronize() {
        mFeatureFlagsSynchronizer.synchronize();

        verify(mRetryTimerSplitsSync).start();
    }

    @Test
    public void stopPeriodicFetching() {
        SplitsSyncTask mockTask = mock(SplitsSyncTask.class);
        when(mTaskFactory.createSplitsSyncTask(false)).thenReturn(mockTask);
        when(mSingleThreadTaskExecutor.schedule(eq(mockTask), anyLong(), anyLong(), any())).thenReturn("12");

        // start periodic fetching to populate task id
        mFeatureFlagsSynchronizer.startPeriodicFetching();
        mFeatureFlagsSynchronizer.stopPeriodicFetching();

        verify(mSingleThreadTaskExecutor).stopTask("12");
    }
}
