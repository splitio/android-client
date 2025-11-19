package io.split.android.client.service.synchronizer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.rules.LoadRuleBasedSegmentsTask;
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
        when(mTaskFactory.createSplitsUpdateTask(1000L, -1L)).thenReturn(task);

        mFeatureFlagsSynchronizer.synchronize(1000L, -1L);

        verify(mRetryTimerSplitsUpdate).setTask(eq(task), argThat(Objects::nonNull));
        verify(mRetryTimerSplitsUpdate).start();
    }

    @Test
    public void synchronizeRuleBasedSegmentsWithSince() {
        SplitsUpdateTask task = mock(SplitsUpdateTask.class);
        when(mTaskFactory.createSplitsUpdateTask((Long) null, 1000L)).thenReturn(task);

        mFeatureFlagsSynchronizer.synchronize(null, 1000L);

        verify(mRetryTimerSplitsUpdate).setTask(eq(task), argThat(Objects::nonNull));
        verify(mTaskFactory).createSplitsUpdateTask((Long) null, 1000L);
        verify(mRetryTimerSplitsUpdate).start();
    }

    @Test
    public void loadAndSynchronizeSplits() {
        LoadSplitsTask mockLoadTask = mock(LoadSplitsTask.class);
        when(mockLoadTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        when(mTaskFactory.createLoadSplitsTask()).thenReturn(mockLoadTask);

        LoadRuleBasedSegmentsTask mockLoadRuleBasedSegmentsTask = mock(LoadRuleBasedSegmentsTask.class);
        when(mockLoadRuleBasedSegmentsTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_RULE_BASED_SEGMENTS));
        when(mTaskFactory.createLoadRuleBasedSegmentsTask()).thenReturn(mockLoadRuleBasedSegmentsTask);

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
        assertEquals(4, argument.getValue().size());
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

    @Test
    public void startPeriodicFetchingCancelsPreviousTaskIfExecutedSequentially() {
        SplitsSyncTask mockTask = mock(SplitsSyncTask.class);
        SplitsSyncTask mockTask2 = mock(SplitsSyncTask.class);
        when(mTaskFactory.createSplitsSyncTask(false))
                .thenReturn(mockTask)
                .thenReturn(mockTask2);
        when(mSingleThreadTaskExecutor.schedule(eq(mockTask), anyLong(), anyLong(), any()))
                .thenReturn("12")
                .thenReturn("13");

        mFeatureFlagsSynchronizer.startPeriodicFetching();
        mFeatureFlagsSynchronizer.startPeriodicFetching();

        verify(mSingleThreadTaskExecutor).schedule(eq(mockTask), anyLong(), anyLong(), any());
        verify(mSingleThreadTaskExecutor).stopTask("12");
        verify(mSingleThreadTaskExecutor).schedule(eq(mockTask2), anyLong(), anyLong(), any());
    }

    @Test
    public void splitsSyncTaskIsStoppedWhenTaskResultIsDoNotRetry() throws InterruptedException {
        SplitsSyncTask mockTask = mock(SplitsSyncTask.class);
        when(mTaskFactory.createSplitsSyncTask(false)).thenReturn(mockTask);
        when(mockTask.execute()).thenReturn(SplitTaskExecutionInfo.error(
                SplitTaskType.SPLITS_SYNC,
                Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true)));

        CountDownLatch latch = new CountDownLatch(1);
        when(mSingleThreadTaskExecutor.schedule(any(), anyLong(), anyLong(), any()))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) {
                        final String returnValue = "12";
                        SplitTaskExecutionListener listener = invocation.getArgument(3);
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            listener.taskExecuted(((SplitTask) invocation.getArgument(0)).execute());
                            latch.countDown();
                        }).start();
                        return returnValue;
                    }
                });

        mFeatureFlagsSynchronizer.startPeriodicFetching();
        latch.await(5, TimeUnit.SECONDS);

        verify(mSingleThreadTaskExecutor).stopTask("12");
    }

    @Test
    public void splitsSyncTaskIsNotRestartedWhenTaskResultIsDoNotRetry() throws InterruptedException {
        SplitsSyncTask mockTask = mock(SplitsSyncTask.class);
        when(mTaskFactory.createSplitsSyncTask(false)).thenReturn(mockTask);
        when(mockTask.execute()).thenReturn(SplitTaskExecutionInfo.error(
                SplitTaskType.SPLITS_SYNC,
                Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true)));

        CountDownLatch latch = new CountDownLatch(1);
        when(mSingleThreadTaskExecutor.schedule(any(), anyLong(), anyLong(), any()))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) {
                        final String returnValue = "12";
                        SplitTaskExecutionListener listener = invocation.getArgument(3);
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            listener.taskExecuted(((SplitTask) invocation.getArgument(0)).execute());
                            latch.countDown();
                        }).start();
                        return returnValue;
                    }
                });

        mFeatureFlagsSynchronizer.startPeriodicFetching();
        latch.await(5, TimeUnit.SECONDS);
        mFeatureFlagsSynchronizer.startPeriodicFetching();

        verify(mSingleThreadTaskExecutor).stopTask("12");
        verify(mSingleThreadTaskExecutor, times(1)).schedule(eq(mockTask), anyLong(), anyLong(), any());
    }
}
