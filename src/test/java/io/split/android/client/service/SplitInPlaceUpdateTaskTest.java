package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import io.split.android.client.dtos.Split;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitInPlaceUpdateTask;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class SplitInPlaceUpdateTaskTest {

    @Mock
    private SplitsStorage mSplitsStorage;
    @Mock
    private SplitChangeProcessor mSplitChangeProcessor;
    @Mock
    private ISplitEventsManager mEventsManager;
    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    @Mock
    private Split mSplit;

    private SplitInPlaceUpdateTask mSplitInPlaceUpdateTask;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        long changeNumber = 123L;
        mSplitInPlaceUpdateTask = new SplitInPlaceUpdateTask(
                mSplitsStorage, mSplitChangeProcessor, mEventsManager,
                mTelemetryRuntimeProducer, mSplit, changeNumber
        );
    }

    @Test
    public void sseUpdateIsRecordedInTelemetryWhenOperationIsSuccessful() {
        ProcessedSplitChange processedSplitChange = new ProcessedSplitChange(new ArrayList<>(), new ArrayList<>(), 0L, 0);

        when(mSplitChangeProcessor.process(mSplit, 123L)).thenReturn(processedSplitChange);

        SplitTaskExecutionInfo result = mSplitInPlaceUpdateTask.execute();

        verify(mSplitChangeProcessor).process(mSplit, 123L);
        verify(mSplitsStorage).update(processedSplitChange);
        verify(mEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
        verify(mTelemetryRuntimeProducer).recordUpdatesFromSSE(UpdatesFromSSEEnum.SPLITS);

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.SUCCESS);
    }

    @Test
    public void exceptionDuringProcessingReturnsErrorExecutionInfo() {

        doThrow(new RuntimeException()).when(mSplitChangeProcessor).process(mSplit, 123L);

        SplitTaskExecutionInfo result = mSplitInPlaceUpdateTask.execute();

        verify(mSplitChangeProcessor).process(mSplit, 123L);
        verify(mSplitsStorage, never()).update(any());
        verify(mEventsManager, never()).notifyInternalEvent(any());
        verify(mTelemetryRuntimeProducer, never()).recordUpdatesFromSSE(any());

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.ERROR);
    }

    @Test
    public void exceptionDuringStorageUpdateReturnsErrorExecutionInfo() {
        ProcessedSplitChange processedSplitChange = new ProcessedSplitChange(new ArrayList<>(), new ArrayList<>(), 0L, 0);

        when(mSplitChangeProcessor.process(mSplit, 123L)).thenReturn(processedSplitChange);
        doThrow(new RuntimeException()).when(mSplitsStorage).update(processedSplitChange);

        SplitTaskExecutionInfo result = mSplitInPlaceUpdateTask.execute();

        verify(mSplitChangeProcessor).process(mSplit, 123L);
        verify(mSplitsStorage).update(processedSplitChange);
        verify(mEventsManager, never()).notifyInternalEvent(any());
        verify(mTelemetryRuntimeProducer, never()).recordUpdatesFromSSE(any());

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.ERROR);
    }
}
