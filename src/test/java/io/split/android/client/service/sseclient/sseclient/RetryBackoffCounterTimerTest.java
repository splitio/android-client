package io.split.android.client.service.sseclient.sseclient;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
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

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        counterTimer = new RetryBackoffCounterTimer(taskExecutor, backoffCounter);
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
}
