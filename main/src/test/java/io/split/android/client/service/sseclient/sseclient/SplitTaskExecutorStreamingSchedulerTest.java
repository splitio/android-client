package io.split.android.client.service.sseclient.sseclient;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.spi.StreamingScheduler;

public class SplitTaskExecutorStreamingSchedulerTest {

    private SplitTaskExecutor mTaskExecutor;
    private SplitTaskExecutorStreamingScheduler mScheduler;

    @Before
    public void setUp() {
        mTaskExecutor = mock(SplitTaskExecutor.class);
        mScheduler = new SplitTaskExecutorStreamingScheduler(mTaskExecutor);
    }

    @Test
    public void scheduleReturnsTaskIdFromExecutor() {
        when(mTaskExecutor.schedule(any(SplitTask.class), eq(10L), any(SplitTaskExecutionListener.class)))
                .thenReturn("task-123");

        String taskId = mScheduler.schedule(() -> {}, 10L, null);

        assertEquals("task-123", taskId);
    }

    @Test
    public void scheduleUsesCorrectDelay() {
        Runnable task = mock(Runnable.class);

        mScheduler.schedule(task, 42L, null);

        verify(mTaskExecutor).schedule(any(SplitTask.class), eq(42L), any(SplitTaskExecutionListener.class));
    }

    @Test
    public void scheduledTaskExecutesRunnable() {
        Runnable task = mock(Runnable.class);
        ArgumentCaptor<SplitTask> taskCaptor = ArgumentCaptor.forClass(SplitTask.class);

        when(mTaskExecutor.schedule(taskCaptor.capture(), eq(10L), any(SplitTaskExecutionListener.class)))
                .thenReturn("task-id");

        mScheduler.schedule(task, 10L, null);

        // Execute the captured SplitTask
        SplitTask splitTask = taskCaptor.getValue();
        splitTask.execute();

        verify(task).run();
    }

    @Test
    public void scheduledTaskReturnsSuccessWhenRunnableCompletesNormally() {
        Runnable task = () -> { /* normal execution */ };
        ArgumentCaptor<SplitTask> taskCaptor = ArgumentCaptor.forClass(SplitTask.class);

        when(mTaskExecutor.schedule(taskCaptor.capture(), anyLong(), any()))
                .thenReturn("task-id");

        mScheduler.schedule(task, 10L, null);

        SplitTask splitTask = taskCaptor.getValue();
        SplitTaskExecutionInfo result = splitTask.execute();

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(SplitTaskType.GENERIC_TASK, result.getTaskType());
    }

    @Test
    public void scheduledTaskReturnsErrorWhenRunnableThrowsException() {
        Runnable task = () -> {
            throw new RuntimeException("Task failed");
        };
        ArgumentCaptor<SplitTask> taskCaptor = ArgumentCaptor.forClass(SplitTask.class);

        when(mTaskExecutor.schedule(taskCaptor.capture(), anyLong(), any()))
                .thenReturn("task-id");

        mScheduler.schedule(task, 10L, null);

        SplitTask splitTask = taskCaptor.getValue();
        SplitTaskExecutionInfo result = splitTask.execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        assertEquals(SplitTaskType.GENERIC_TASK, result.getTaskType());
    }

    @Test
    public void listenerIsCalledWhenTaskCompletes() {
        StreamingScheduler.TaskExecutionListener listener = mock(StreamingScheduler.TaskExecutionListener.class);
        ArgumentCaptor<SplitTaskExecutionListener> listenerCaptor =
                ArgumentCaptor.forClass(SplitTaskExecutionListener.class);

        when(mTaskExecutor.schedule(any(SplitTask.class), anyLong(), listenerCaptor.capture()))
                .thenReturn("task-id");

        mScheduler.schedule(() -> {}, 10L, listener);

        // Simulate task completion
        SplitTaskExecutionListener splitListener = listenerCaptor.getValue();
        splitListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        verify(listener).onTaskExecuted();
    }

    @Test
    public void listenerIsNotCalledWhenNull() {
        ArgumentCaptor<SplitTaskExecutionListener> listenerCaptor =
                ArgumentCaptor.forClass(SplitTaskExecutionListener.class);

        when(mTaskExecutor.schedule(any(SplitTask.class), anyLong(), listenerCaptor.capture()))
                .thenReturn("task-id");

        // Schedule with null listener - should not throw
        mScheduler.schedule(() -> {}, 10L, null);

        // Simulate task completion - should not throw
        SplitTaskExecutionListener splitListener = listenerCaptor.getValue();
        splitListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        // No exception means test passes
    }

    @Test
    public void cancelWithNullTaskIdDoesNotCallStopTask() {
        mScheduler.cancel(null);

        // When taskId is null, stopTask should not be called
        verify(mTaskExecutor, never()).stopTask(any());
    }

    @Test
    public void cancelWithNonNullTaskIdCallsStopTask() {
        mScheduler.cancel("task-456");

        verify(mTaskExecutor).stopTask("task-456");
    }

    @Test
    public void scheduledTaskHandlesDifferentExceptionTypes() {
        // Test with different exception types to ensure all are caught
        Runnable task1 = () -> {
            throw new IllegalArgumentException("Invalid argument");
        };
        Runnable task2 = () -> {
            throw new NullPointerException("Null pointer");
        };

        ArgumentCaptor<SplitTask> taskCaptor = ArgumentCaptor.forClass(SplitTask.class);
        when(mTaskExecutor.schedule(taskCaptor.capture(), anyLong(), any()))
                .thenReturn("task-id");

        // Test IllegalArgumentException
        mScheduler.schedule(task1, 10L, null);
        SplitTask splitTask1 = taskCaptor.getValue();
        SplitTaskExecutionInfo result1 = splitTask1.execute();
        assertEquals(SplitTaskExecutionStatus.ERROR, result1.getStatus());

        // Test NullPointerException
        mScheduler.schedule(task2, 10L, null);
        SplitTask splitTask2 = taskCaptor.getAllValues().get(1);
        SplitTaskExecutionInfo result2 = splitTask2.execute();
        assertEquals(SplitTaskExecutionStatus.ERROR, result2.getStatus());
    }

    @Test
    public void multipleScheduleCallsWorkIndependently() {
        when(mTaskExecutor.schedule(any(SplitTask.class), eq(10L), any()))
                .thenReturn("task-1");
        when(mTaskExecutor.schedule(any(SplitTask.class), eq(20L), any()))
                .thenReturn("task-2");

        String taskId1 = mScheduler.schedule(() -> {}, 10L, null);
        String taskId2 = mScheduler.schedule(() -> {}, 20L, null);

        assertEquals("task-1", taskId1);
        assertEquals("task-2", taskId2);
        verify(mTaskExecutor).schedule(any(SplitTask.class), eq(10L), any());
        verify(mTaskExecutor).schedule(any(SplitTask.class), eq(20L), any());
    }

    @Test
    public void scheduleWithZeroDelay() {
        when(mTaskExecutor.schedule(any(SplitTask.class), eq(0L), any()))
                .thenReturn("immediate-task");

        String taskId = mScheduler.schedule(() -> {}, 0L, null);

        assertEquals("immediate-task", taskId);
        verify(mTaskExecutor).schedule(any(SplitTask.class), eq(0L), any());
    }

    @Test
    public void scheduleWithLargeDelay() {
        long largeDelay = 3600L; // 1 hour
        when(mTaskExecutor.schedule(any(SplitTask.class), eq(largeDelay), any()))
                .thenReturn("delayed-task");

        String taskId = mScheduler.schedule(() -> {}, largeDelay, null);

        assertEquals("delayed-task", taskId);
        verify(mTaskExecutor).schedule(any(SplitTask.class), eq(largeDelay), any());
    }

    @Test
    public void listenerReceivesTaskInfoRegardlessOfStatus() {
        StreamingScheduler.TaskExecutionListener listener = mock(StreamingScheduler.TaskExecutionListener.class);
        ArgumentCaptor<SplitTaskExecutionListener> listenerCaptor =
                ArgumentCaptor.forClass(SplitTaskExecutionListener.class);

        when(mTaskExecutor.schedule(any(SplitTask.class), anyLong(), listenerCaptor.capture()))
                .thenReturn("task-id");

        mScheduler.schedule(() -> {}, 10L, listener);
        SplitTaskExecutionListener splitListener = listenerCaptor.getValue();

        // Test with success status
        splitListener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        verify(listener).onTaskExecuted();

        // Test with error status
        splitListener.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.GENERIC_TASK));
        verify(listener, times(2)).onTaskExecuted(); // Should be called twice now
    }
}
