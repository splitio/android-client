package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTaskConfig;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class MySegmentsUpdateTaskTest {

    @Mock
    MySegmentsStorage mySegmentsStorage;

    @Mock
    SplitEventsManager mEventsManager;

    MySegmentsUpdateTask mTask;

    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    String mSegmentToRemove = "MS_TO_REMOVE";
    String mCustomerSegment = "CUSTOMER_ID";

    @Before
    public void setup() {
        Set<String> oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);
        oldSegments.add(mSegmentToRemove);

        MockitoAnnotations.initMocks(this);
        when(mySegmentsStorage.getAll()).thenReturn(oldSegments);
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, Collections.singleton(mSegmentToRemove), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(segmentsCaptor.capture());
        Assert.assertTrue(isSegmentRemoved(segmentsCaptor.getValue(), mSegmentToRemove));
        Assert.assertFalse(isSegmentRemoved(segmentsCaptor.getValue(), mCustomerSegment));
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_UPDATE, result.getTaskType());
    }

    @Test
    public void correctExecutionToEraseNotInSegments() throws HttpFetcherException {
        String otherSegment = "OtherSegment";
        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, Collections.singleton(otherSegment), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());
        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, never()).set(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_UPDATE, result.getTaskType());
    }

    @Test
    public void storageException() {

        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, Collections.singleton(mSegmentToRemove), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(any());

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        verifyNoInteractions(mTelemetryRuntimeProducer);
    }

    @Test
    public void successfulAddOperationIsRecordedInTelemetry() {
        mTask = new MySegmentsUpdateTask(mySegmentsStorage, true, Collections.singleton(mSegmentToRemove), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordUpdatesFromSSE(UpdatesFromSSEEnum.MY_SEGMENTS);
    }

    @Test
    public void successfulRemoveOperationIsRecordedInTelemetry() {
        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, Collections.singleton(mSegmentToRemove), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordUpdatesFromSSE(UpdatesFromSSEEnum.MY_SEGMENTS);
    }

    @Test
    public void addOperationWithSegmentsAlreadyInStorage() {
        Set<String> oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);
        oldSegments.add(mSegmentToRemove);

        when(mySegmentsStorage.getAll()).thenReturn(oldSegments);

        mTask = new MySegmentsUpdateTask(mySegmentsStorage, true, oldSegments, mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, never()).set(any());
        verify(mEventsManager, never()).notifyInternalEvent(any());
    }

    @Test
    public void addOperationWithOneSegmentAlreadyInStorage() {
        Set<String> oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);

        when(mySegmentsStorage.getAll()).thenReturn(oldSegments);

        mTask = new MySegmentsUpdateTask(mySegmentsStorage, true, new HashSet<>(Arrays.asList(mCustomerSegment, mSegmentToRemove)), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(segmentsCaptor.capture());
        Assert.assertTrue(segmentsCaptor.getValue().contains(mSegmentToRemove));
        Assert.assertTrue(segmentsCaptor.getValue().contains(mCustomerSegment));
        Assert.assertEquals(2, segmentsCaptor.getValue().size());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_UPDATE, result.getTaskType());
    }

    @Test
    public void removeOperationRemovesOnlyNotifiedSegments() {
        Set<String> oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);
        oldSegments.add(mSegmentToRemove);
        oldSegments.add("extra_segment");

        when(mySegmentsStorage.getAll()).thenReturn(oldSegments);

        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, new HashSet<>(Arrays.asList(mSegmentToRemove, "extra_segment")), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(segmentsCaptor.capture());
        Assert.assertFalse(segmentsCaptor.getValue().contains(mSegmentToRemove));
        Assert.assertFalse(segmentsCaptor.getValue().contains("extra_segment"));
        Assert.assertTrue(segmentsCaptor.getValue().contains(mCustomerSegment));
        Assert.assertEquals(1, segmentsCaptor.getValue().size());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        verify(mEventsManager).notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
    }

    @Test
    public void removeOperationDoesNotNotifyWhenNothingWasRemoved() {
        Set<String> oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);
        oldSegments.add(mSegmentToRemove);

        when(mySegmentsStorage.getAll()).thenReturn(oldSegments);

        mTask = new MySegmentsUpdateTask(mySegmentsStorage, false, Collections.singleton("extra_segment"), mEventsManager, mTelemetryRuntimeProducer, MySegmentsUpdateTaskConfig.getForMySegments());

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mEventsManager, never()).notifyInternalEvent(any());
    }

    @After
    public void tearDown() {
        reset(mySegmentsStorage);
    }

    private boolean isSegmentRemoved(List<String> segments, String segment) {
        return !(new HashSet<>(segments).contains(segment));
    }
}
