package io.split.android.client.service.sseclient.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.sseclient.spi.StreamingScheduler;

public class SseDisconnectionTimerTest {

    private StreamingScheduler mScheduler;
    private Runnable mTask;
    private SseDisconnectionTimer mSseDisconnectionTimer;

    @Before
    public void setUp() {
        mScheduler = mock(StreamingScheduler.class);
        mTask = mock(Runnable.class);
        mSseDisconnectionTimer = new SseDisconnectionTimer(mScheduler, 0);
    }

    @Test
    public void cancelCallsSchedulerCancelWithNull() {
        // When no task has been scheduled, mTaskId is null
        mSseDisconnectionTimer.cancel();

        verify(mScheduler).cancel(isNull());
    }

    @Test
    public void scheduleSchedulesTaskInScheduler() {
        mSseDisconnectionTimer.schedule(mTask);

        // schedule() internally calls cancel() first, then schedules the task
        verify(mScheduler).schedule(eq(mTask), eq(0L), any());
    }

    @Test
    public void cancelCancelsTaskWithCorrectTaskId() {
        when(mScheduler.schedule(eq(mTask), anyLong(), any())).thenReturn("task-id");

        mSseDisconnectionTimer.schedule(mTask);
        mSseDisconnectionTimer.cancel();

        // Second cancel call should use the task ID returned by schedule
        verify(mScheduler).cancel("task-id");
    }

    @Test
    public void scheduleInitialDelayInSecondsUsesProvidedValue() {
        mSseDisconnectionTimer = new SseDisconnectionTimer(mScheduler, 60);

        mSseDisconnectionTimer.schedule(mTask);
        verify(mScheduler).schedule(eq(mTask), eq(60L), any());
    }
}
