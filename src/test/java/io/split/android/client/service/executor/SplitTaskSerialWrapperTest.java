package io.split.android.client.service.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.List;

public class SplitTaskSerialWrapperTest {

    @Test
    public void successfulStatusContainsResultsOfEveryTask() {
        SplitTask task1 = mock(SplitTask.class);
        SplitTask task2 = mock(SplitTask.class);

        when(task1.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK));
        when(task2.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_STATS_TASK));

        SplitTaskSerialWrapper wrapper = new SplitTaskSerialWrapper(task1, task2);

        SplitTaskExecutionInfo executionInfo = wrapper.execute();

        List<SplitTaskExecutionInfo> results = (List<SplitTaskExecutionInfo>) executionInfo.getObjectValue("serial_task_results");
        assertEquals(SplitTaskExecutionStatus.SUCCESS, executionInfo.getStatus());
        assertEquals(2, results.size());
        assertEquals(SplitTaskType.TELEMETRY_CONFIG_TASK, results.get(0).getTaskType());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, results.get(0).getStatus());

        assertEquals(SplitTaskType.TELEMETRY_STATS_TASK, results.get(1).getTaskType());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, results.get(1).getStatus());
    }

    @Test
    public void unsuccessfulResultContainsExecutionInfoUpToFirstUnsuccessfulTask() {
        SplitTask task1 = mock(SplitTask.class);
        SplitTask task2 = mock(SplitTask.class);
        SplitTask task3 = mock(SplitTask.class);

        when(task1.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK));
        when(task2.execute()).thenReturn(SplitTaskExecutionInfo.error(SplitTaskType.TELEMETRY_STATS_TASK));
        when(task3.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER));

        SplitTaskSerialWrapper wrapper = new SplitTaskSerialWrapper(task1, task2, task3);

        SplitTaskExecutionInfo executionInfo = wrapper.execute();

        List<SplitTaskExecutionInfo> results = (List<SplitTaskExecutionInfo>) executionInfo.getObjectValue("serial_task_results");
        assertEquals(SplitTaskExecutionStatus.ERROR, executionInfo.getStatus());
        assertEquals(2, results.size());
        assertEquals(SplitTaskType.TELEMETRY_CONFIG_TASK, results.get(0).getTaskType());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, results.get(0).getStatus());

        assertEquals(SplitTaskType.TELEMETRY_STATS_TASK, results.get(1).getTaskType());
        assertEquals(SplitTaskExecutionStatus.ERROR, results.get(1).getStatus());
    }

    @Test
    public void successfulTasksAreAllExecuted() {
        SplitTask task1 = mock(SplitTask.class);
        SplitTask task2 = mock(SplitTask.class);
        SplitTask task3 = mock(SplitTask.class);

        when(task1.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK));
        when(task2.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_STATS_TASK));
        when(task3.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER));

        SplitTaskSerialWrapper wrapper = new SplitTaskSerialWrapper(task1, task2, task3);

        wrapper.execute();

        verify(task1).execute();
        verify(task2).execute();
        verify(task3).execute();
    }

    @Test
    public void tasksAreExecutedUpToUnsuccessfulOne() {
        SplitTask task1 = mock(SplitTask.class);
        SplitTask task2 = mock(SplitTask.class);
        SplitTask task3 = mock(SplitTask.class);
        SplitTask task4 = mock(SplitTask.class);

        when(task1.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK));
        when(task2.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_STATS_TASK));
        when(task3.execute()).thenReturn(SplitTaskExecutionInfo.error(SplitTaskType.IMPRESSIONS_RECORDER));
        when(task4.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER));

        SplitTaskSerialWrapper wrapper = new SplitTaskSerialWrapper(task1, task2, task3, task4);
        wrapper.execute();

        verify(task1).execute();
        verify(task2).execute();
        verify(task3).execute();
        verify(task4, never()).execute();
    }

}
