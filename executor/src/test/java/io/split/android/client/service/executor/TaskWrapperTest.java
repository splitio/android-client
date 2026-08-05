package io.split.android.client.service.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TaskWrapperTest {

    @Test
    public void listenerIsNotifiedWhenTaskCompletesSuccessfully() {
        SplitTask task = mock(SplitTask.class);
        SplitTaskExecutionListener listener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo info = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        when(task.execute()).thenReturn(info);

        new TaskWrapper(task, listener).run();

        verify(listener, times(1)).taskExecuted(info);
    }

    @Test
    public void listenerIsNotifiedWithErrorInfoWhenTaskThrows() {
        SplitTask task = mock(SplitTask.class);
        SplitTaskExecutionListener listener = mock(SplitTaskExecutionListener.class);
        when(task.execute()).thenThrow(new RuntimeException("boom"));

        new TaskWrapper(task, listener).run();

        ArgumentCaptor<SplitTaskExecutionInfo> captor = ArgumentCaptor.forClass(SplitTaskExecutionInfo.class);
        verify(listener, times(1)).taskExecuted(captor.capture());
        assertEquals(SplitTaskExecutionStatus.ERROR, captor.getValue().getStatus());
    }
}
