package io.split.android.client.service.sseclient.sseclient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;

public class SseDisconnectionTimerTest {

    private SplitTaskExecutor mTaskExecutor;
    private SplitTask mTask;
    private SseDisconnectionTimer mSseDisconnectionTimer;

    @Before
    public void setUp() {
        mTaskExecutor = mock(SplitTaskExecutor.class);
        mTask = mock(SplitTask.class);
        when(mTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        mSseDisconnectionTimer = new SseDisconnectionTimer(mTaskExecutor, 0);
    }

    @Test
    public void cancelDoesNothingWhenTaskHasNotBeenScheduled() {
        mSseDisconnectionTimer.cancel();

        verify(mTaskExecutor, times(0)).stopTask(any());
    }

    @Test
    public void scheduleSchedulesTaskInTaskExecutor() {
        mSseDisconnectionTimer.schedule(mTask);

        verify(mTaskExecutor).schedule(eq(mTask), eq(0L), eq(mSseDisconnectionTimer));
    }

    @Test
    public void cancelCancelsTaskWithCorrectTaskId() {
        when(mTaskExecutor.schedule(eq(mTask), anyLong(), any())).thenReturn("id");

        mSseDisconnectionTimer.schedule(mTask);
        mSseDisconnectionTimer.cancel();

        verify(mTaskExecutor).stopTask("id");
    }

    @Test
    public void scheduleInitialDelayInSecondsDefaultValueIs60() {
        mSseDisconnectionTimer = new SseDisconnectionTimer(mTaskExecutor, 60);

        mSseDisconnectionTimer.schedule(mTask);
        verify(mTaskExecutor).schedule(mTask, 60L, mSseDisconnectionTimer);
    }
}
