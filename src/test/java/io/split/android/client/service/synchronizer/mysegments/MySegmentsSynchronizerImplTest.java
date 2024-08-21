package io.split.android.client.service.synchronizer.mysegments;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;

public class MySegmentsSynchronizerImplTest {

    @Mock
    private RetryBackoffCounterTimer mRetryBackoffCounterTimer;
    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitEventsManager mSplitEventsManager;
    @Mock
    private MySegmentsTaskFactory mMySegmentsTaskFactory;
    private static final int SEGMENTS_REFRESH_RATE = 1;
    private MySegmentsSynchronizerImpl mMySegmentsSynchronizer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mMySegmentsSynchronizer = new MySegmentsSynchronizerImpl(mRetryBackoffCounterTimer,
                mSplitTaskExecutor,
                mSplitEventsManager,
                mMySegmentsTaskFactory,
                SEGMENTS_REFRESH_RATE, SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
    }

    @Test
    public void loadMySegmentsFromCacheSubmitsTasksToTaskExecutor() {
        LoadMySegmentsTask mockTask = mock(LoadMySegmentsTask.class);
        when(mMySegmentsTaskFactory.createLoadMySegmentsTask()).thenReturn(mockTask);

        mMySegmentsSynchronizer.loadMySegmentsFromCache();

        verify(mSplitTaskExecutor).submit(eq(mockTask), any());
    }

    @Test
    public void synchronizeMySegmentsStartsSegmentsSyncTask() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false)).thenReturn(mockTask);

        mMySegmentsSynchronizer.synchronizeMySegments();

        verify(mRetryBackoffCounterTimer).setTask(eq(mockTask), any());
        verify(mRetryBackoffCounterTimer).start();
    }

    @Test
    public void forceMySegmentsSyncLaunchesSegmentsSyncTaskAvoidingCache() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(true)).thenReturn(mockTask);

        mMySegmentsSynchronizer.forceMySegmentsSync(0L);

        verify(mRetryBackoffCounterTimer).setTask(eq(mockTask), eq(0L), any());
        verify(mRetryBackoffCounterTimer).start();
    }

    @Test
    public void forceMySegmentsSyncStopsPreviouslyRunningTask() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(true)).thenReturn(mockTask);

        mMySegmentsSynchronizer.forceMySegmentsSync(1000L);
        mMySegmentsSynchronizer.forceMySegmentsSync(0L);

        verify(mRetryBackoffCounterTimer).setTask(eq(mockTask), eq(1000L), any());
        verify(mRetryBackoffCounterTimer).setTask(eq(mockTask), eq(0L), any());
        verify(mRetryBackoffCounterTimer, times(2)).stop();
        verify(mRetryBackoffCounterTimer, times(2)).start();
    }

    @Test
    public void scheduleSegmentsSyncTaskSchedulesSyncTaskInTaskExecutor() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false)).thenReturn(mockTask);
        when(mSplitTaskExecutor.schedule(eq(mockTask), eq(1L), eq(1L), notNull())).thenReturn("TaskID");

        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();

        verify(mSplitTaskExecutor).schedule(eq(mockTask), eq(1L), eq(1L), notNull());
    }

    @Test
    public void stopPeriodicFetchingCallsStopTaskOnExecutor() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false)).thenReturn(mockTask);
        when(mSplitTaskExecutor.schedule(eq(mockTask), eq(1L), eq(1L), notNull())).thenReturn("TaskID");

        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();
        mMySegmentsSynchronizer.stopPeriodicFetching();

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mSplitTaskExecutor).stopTask(argumentCaptor.capture());
        assertEquals("TaskID", argumentCaptor.getValue());
    }

    @Test
    public void startPeriodicFetchingCancelsPreviousTaskIfExecutedSequentially() {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        MySegmentsSyncTask mockTask2 = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false))
                .thenReturn(mockTask)
                .thenReturn(mockTask2);
        when(mSplitTaskExecutor.schedule(eq(mockTask), eq(1L), eq(1L), notNull())).thenReturn("TaskID");
        when(mSplitTaskExecutor.schedule(eq(mockTask2), eq(1L), eq(1L), notNull())).thenReturn("TaskID2");

        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();
        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();

        verify(mSplitTaskExecutor).schedule(eq(mockTask), eq(1L), eq(1L), notNull());
        verify(mSplitTaskExecutor, times(0)).stopTask("TaskID");
        verify(mSplitTaskExecutor, times(0)).schedule(eq(mockTask2), eq(1L), eq(1L), notNull());
        verify(mSplitTaskExecutor, times(0)).stopTask("TaskID2");
    }

    @Test
    public void syncTaskIsStoppedWhenTaskResultIsDoNotRetry() throws InterruptedException {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false)).thenReturn(mockTask);
        when(mockTask.execute()).thenReturn(SplitTaskExecutionInfo.error(
                SplitTaskType.MY_SEGMENTS_SYNC,
                Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true)));

        CountDownLatch latch = new CountDownLatch(1);
        when(mSplitTaskExecutor.schedule(any(), anyLong(), anyLong(), any()))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) {
                        final String returnValue = "TaskId";
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

        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();
        latch.await(5, TimeUnit.SECONDS);

        verify(mSplitTaskExecutor).stopTask("TaskId");
    }

    @Test
    public void syncTaskIsNotRestartedWhenTaskResultIsDoNotRetry() throws InterruptedException {
        MySegmentsSyncTask mockTask = mock(MySegmentsSyncTask.class);
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(false)).thenReturn(mockTask);
        when(mockTask.execute()).thenReturn(SplitTaskExecutionInfo.error(
                SplitTaskType.MY_SEGMENTS_SYNC,
                Collections.singletonMap(SplitTaskExecutionInfo.DO_NOT_RETRY, true)));

        CountDownLatch latch = new CountDownLatch(1);
        when(mSplitTaskExecutor.schedule(any(), anyLong(), anyLong(), any()))
                .thenAnswer(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) {
                        final String returnValue = "TaskId";
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

        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();
        latch.await(5, TimeUnit.SECONDS);
        mMySegmentsSynchronizer.scheduleSegmentsSyncTask();

        verify(mSplitTaskExecutor, times(1)).schedule(any(), anyLong(), anyLong(), any());
    }


    @Test
    public void destroyCallsStopOnSyncRetryTimer() {
        mMySegmentsSynchronizer.destroy();

        verify(mRetryBackoffCounterTimer).stop();
    }
}
