package io.split.android.client.service.executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SplitTaskBatchWrapperTest {

    @Test
    public void laterItemsRunAfterATaskThrows() {
        SplitTask firstTask = mock(SplitTask.class);
        SplitTask throwingTask = mock(SplitTask.class);
        SplitTask thirdTask = mock(SplitTask.class);
        SplitTaskExecutionListener firstListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener throwingListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener thirdListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo firstInfo = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        SplitTaskExecutionInfo thirdInfo = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        when(firstTask.execute()).thenReturn(firstInfo);
        when(throwingTask.execute()).thenThrow(new RuntimeException("boom"));
        when(thirdTask.execute()).thenReturn(thirdInfo);

        List<SplitTaskBatchItem> items = Arrays.asList(
                new SplitTaskBatchItem(firstTask, firstListener),
                new SplitTaskBatchItem(throwingTask, throwingListener),
                new SplitTaskBatchItem(thirdTask, thirdListener));

        new SplitTaskBatchWrapper(items).run();

        verify(firstTask, times(1)).execute();
        verify(firstListener, times(1)).taskExecuted(firstInfo);
        verify(thirdTask, times(1)).execute();
        verify(thirdListener, times(1)).taskExecuted(thirdInfo);
    }

    @Test
    public void throwingListenerDoesNotSkipLaterItems() {
        SplitTask firstTask = mock(SplitTask.class);
        SplitTask secondTask = mock(SplitTask.class);
        SplitTaskExecutionListener throwingListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener secondListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo firstInfo = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        SplitTaskExecutionInfo secondInfo = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        when(firstTask.execute()).thenReturn(firstInfo);
        when(secondTask.execute()).thenReturn(secondInfo);
        doThrow(new RuntimeException("listener boom"))
                .when(throwingListener).taskExecuted(firstInfo);

        List<SplitTaskBatchItem> items = Arrays.asList(
                new SplitTaskBatchItem(firstTask, throwingListener),
                new SplitTaskBatchItem(secondTask, secondListener));

        new SplitTaskBatchWrapper(items).run();

        verify(throwingListener, times(1)).taskExecuted(firstInfo);
        verify(secondTask, times(1)).execute();
        verify(secondListener, times(1)).taskExecuted(secondInfo);
    }

    @Test
    public void listenerOfThrowingItemIsNotCalled() {
        SplitTask throwingTask = mock(SplitTask.class);
        SplitTask nextTask = mock(SplitTask.class);
        SplitTaskExecutionListener throwingItemListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionListener nextListener = mock(SplitTaskExecutionListener.class);
        SplitTaskExecutionInfo nextInfo = SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        when(throwingTask.execute()).thenThrow(new RuntimeException("boom"));
        when(nextTask.execute()).thenReturn(nextInfo);

        List<SplitTaskBatchItem> items = Arrays.asList(
                new SplitTaskBatchItem(throwingTask, throwingItemListener),
                new SplitTaskBatchItem(nextTask, nextListener));

        new SplitTaskBatchWrapper(items).run();

        verify(throwingItemListener, never()).taskExecuted(any());
        verify(nextListener, times(1)).taskExecuted(nextInfo);
    }
}
