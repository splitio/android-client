package io.split.android.client.service.sseclient.sseclient;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.BackoffCounter;

public class RetryBackoffCounterTimerTest {

    @Mock
    private SplitTaskExecutor taskExecutor;
    @Mock
    private BackoffCounter backoffCounter;
    @Mock
    private SplitTask mockTask;
    private RetryBackoffCounterTimer counterTimer;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter);
    }

    @After
    public void tearDown() {
        try {
            mAutoCloseable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void stopCallsStopInTaskExecutorWhenTaskIsNotNull() {
        when(taskExecutor.schedule(mockTask,
                0L,
                counterTimer)).thenReturn("100");
        counterTimer.setTask(mockTask);
        counterTimer.start();

        counterTimer.stop();

        verify(taskExecutor).stopTask("100");
    }

    @Test
    public void startSchedulesTaskInExecutor() {
        counterTimer.setTask(mockTask);
        counterTimer.start();

        verify(taskExecutor).schedule(mockTask, 0L, counterTimer);
    }

    @Test
    public void startResetsCounterInBackoffCounterWhenTaskIsNotNull() {
        counterTimer.setTask(mockTask);

        counterTimer.start();

        verify(backoffCounter).resetCounter();
    }

    @Test
    public void taskIsReScheduledOnFailureUpToTheSpecifiedLimit() {
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter, 3);

        when(taskExecutor.schedule(mockTask,
                0L,
                counterTimer)).then(invocation -> {
            counterTimer.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK));
            return "100";
        });

        counterTimer.setTask(mockTask);

        counterTimer.start();

        verify(taskExecutor, times(3)).schedule(mockTask, 0L, counterTimer);
    }

    @Test
    public void testWithListener() throws InterruptedException {
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter, 2);
        when(taskExecutor.schedule(mockTask,
                0L,
                counterTimer))
                .then(invocation -> {
                    counterTimer.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK));
                    return "100";
                })
                .then(invocation -> {
                    counterTimer.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
                    return "100";
                });

        counterTimer.setTask(mockTask, new SplitTaskExecutionListener() {
            @Override
            public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
                listenerLatch.countDown();
            }
        });

        counterTimer.start();
        boolean await = listenerLatch.await(10, TimeUnit.SECONDS);

        assertTrue(await);
        verify(taskExecutor, times(2)).schedule(mockTask, 0L, counterTimer);
    }

    @Test
    public void nonRetryableErrorTaskIsNotRetried() {
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter);

        SplitTaskExecutionListener mockListener = mock(SplitTaskExecutionListener.class);
        when(taskExecutor.schedule(mockTask,
                0L,
                counterTimer)).then(invocation -> {
            counterTimer.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC, Collections.singletonMap("DO_NOT_RETRY", true)));
            return "100";
        });

        counterTimer.setTask(mockTask, mockListener);

        counterTimer.start();

        verify(taskExecutor).schedule(mockTask, 0L, counterTimer);
    }

    @Test
    public void nonRetryableErrorTaskNotifiesListenerWithErrorStatus() {
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter);

        SplitTaskExecutionListener mockListener = mock(SplitTaskExecutionListener.class);
        when(taskExecutor.schedule(mockTask,
                0L,
                counterTimer)).then(invocation -> {
            counterTimer.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC, Collections.singletonMap("DO_NOT_RETRY", true)));
            return "100";
        });

        counterTimer.setTask(mockTask, mockListener);

        counterTimer.start();

        verify(mockListener).taskExecuted(argThat(taskInfo -> taskInfo.getStatus() == SplitTaskExecutionStatus.ERROR &&
                taskInfo.getTaskType() == SplitTaskType.SPLITS_SYNC));
    }
}
