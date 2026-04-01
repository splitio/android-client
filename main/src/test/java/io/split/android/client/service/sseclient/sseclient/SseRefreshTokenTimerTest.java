package io.split.android.client.service.sseclient.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.spi.StreamingScheduler;

public class SseRefreshTokenTimerTest {

    private StreamingScheduler mScheduler;
    private PushManagerEventBroadcaster mBroadcaster;
    private SseRefreshTokenTimer mTimer;

    @Before
    public void setUp() {
        mScheduler = mock(StreamingScheduler.class);
        mBroadcaster = mock(PushManagerEventBroadcaster.class);
        mTimer = new SseRefreshTokenTimer(mScheduler, mBroadcaster);
    }

    @Test
    public void cancelCallsSchedulerCancelWithNull() {
        // When no task has been scheduled, mTaskId is null
        mTimer.cancel();

        verify(mScheduler).cancel(isNull());
    }

    @Test
    public void cancelCancelsTaskWithCorrectTaskId() {
        when(mScheduler.schedule(any(Runnable.class), eq(400L), any())).thenReturn("task-id");

        mTimer.schedule(1000L, 2000L);
        mTimer.cancel();

        // Second cancel call should use the task ID returned by schedule
        verify(mScheduler).cancel("task-id");
    }

    @Test
    public void scheduleCalculatesCorrectReconnectTime() {
        long issueTime = 1000L;
        long expirationTime = 2000L;
        // Expected: (2000 - 1000) - 600 = 400 seconds

        mTimer.schedule(issueTime, expirationTime);

        verify(mScheduler).schedule(any(Runnable.class), eq(400L), any());
    }

    @Test
    public void scheduleReturnsZeroWhenTokenLifetimeLessThan600Seconds() {
        long issueTime = 1000L;
        long expirationTime = 1500L;
        // Expected: (1500 - 1000) - 600 = -100, should be max(0, -100) = 0

        mTimer.schedule(issueTime, expirationTime);

        verify(mScheduler).schedule(any(Runnable.class), eq(0L), any());
    }

    @Test
    public void scheduleReturnsZeroWhenTokenLifetimeEquals600Seconds() {
        long issueTime = 0L;
        long expirationTime = 600L;
        // Expected: (600 - 0) - 600 = 0

        mTimer.schedule(issueTime, expirationTime);

        verify(mScheduler).schedule(any(Runnable.class), eq(0L), any());
    }

    @Test
    public void scheduleCancelsPreviousTaskBeforeSchedulingNew() {
        when(mScheduler.schedule(any(Runnable.class), eq(400L), any())).thenReturn("first-task");

        mTimer.schedule(1000L, 2000L);
        mTimer.schedule(2000L, 3000L);

        // First cancel is with null, second cancel should use "first-task"
        verify(mScheduler).cancel(isNull());
        verify(mScheduler).cancel("first-task");
    }

    @Test
    public void taskExecutionBroadcastsRetryableError() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(mScheduler.schedule(runnableCaptor.capture(), eq(400L), any())).thenReturn("task-id");

        mTimer.schedule(1000L, 2000L);

        // Execute the scheduled task
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Verify that the broadcaster receives a PUSH_RETRYABLE_ERROR event
        ArgumentCaptor<PushStatusEvent> eventCaptor = ArgumentCaptor.forClass(PushStatusEvent.class);
        verify(mBroadcaster).pushMessage(eventCaptor.capture());

        PushStatusEvent event = eventCaptor.getValue();
        assert event.getMessage() == PushStatusEvent.EventType.PUSH_RETRYABLE_ERROR;
    }

    @Test
    public void taskExecutionListenerClearsTaskId() {
        ArgumentCaptor<StreamingScheduler.TaskExecutionListener> listenerCaptor =
            ArgumentCaptor.forClass(StreamingScheduler.TaskExecutionListener.class);
        when(mScheduler.schedule(any(Runnable.class), eq(400L), listenerCaptor.capture())).thenReturn("task-id");

        mTimer.schedule(1000L, 2000L);

        // Execute the task execution listener
        StreamingScheduler.TaskExecutionListener listener = listenerCaptor.getValue();
        listener.onTaskExecuted();

        // After listener is called, next cancel should use null (task ID cleared)
        mTimer.cancel();
        verify(mScheduler, times(2)).cancel(isNull()); // Once during schedule, once in final cancel
    }

    @Test
    public void scheduleWithLargeTokenLifetime() {
        long issueTime = 0L;
        long expirationTime = 3600L; // 1 hour
        // Expected: (3600 - 0) - 600 = 3000 seconds

        mTimer.schedule(issueTime, expirationTime);

        verify(mScheduler).schedule(any(Runnable.class), eq(3000L), any());
    }

    @Test
    public void multipleScheduleCalls() {
        when(mScheduler.schedule(any(Runnable.class), eq(400L), any())).thenReturn("task-1");
        when(mScheduler.schedule(any(Runnable.class), eq(500L), any())).thenReturn("task-2");
        when(mScheduler.schedule(any(Runnable.class), eq(600L), any())).thenReturn("task-3");

        mTimer.schedule(1000L, 2000L); // 400s
        mTimer.schedule(1000L, 2100L); // 500s
        mTimer.schedule(1000L, 2200L); // 600s

        // Each schedule should cancel the previous task
        verify(mScheduler).cancel(isNull());     // First schedule
        verify(mScheduler).cancel("task-1");      // Second schedule
        verify(mScheduler).cancel("task-2");      // Third schedule
    }
}
