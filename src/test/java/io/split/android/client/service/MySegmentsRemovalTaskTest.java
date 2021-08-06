package io.split.android.client.service;

import net.bytebuddy.asm.Advice;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsRemovalTask;
import io.split.android.client.service.mysegments.MySegmentsRemovalTask;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MySegmentsRemovalTaskTest {

    @Mock
    MySegmentsStorage mySegmentsStorage;

    @Mock
    SplitEventsManager mEventsManager;

    MySegmentsRemovalTask mTask;

    String mSegmentToRemove = "MS_TO_REMOVE";
    String mCustomerSegment = "CUSTOMER_ID";

    @Before
    public void setup() {
        Set oldSegments = new HashSet<>();
        oldSegments.add(mCustomerSegment);
        oldSegments.add(mSegmentToRemove);

        MockitoAnnotations.initMocks(this);
       when(mySegmentsStorage.getAll()).thenReturn(oldSegments);
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        mTask = new MySegmentsRemovalTask(mySegmentsStorage, mSegmentToRemove, mEventsManager);

        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(segmentsCaptor.capture());
        Assert.assertTrue(isSegmentRemoved(segmentsCaptor.getValue(), mSegmentToRemove));
        Assert.assertFalse(isSegmentRemoved(segmentsCaptor.getValue(), mCustomerSegment));
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_REMOVAL, result.getTaskType());
    }

    @Test
    public void correctExecutionToEraseNotInSegments() throws HttpFetcherException {
        String otherSegment = "OtherSegment";
        mTask = new MySegmentsRemovalTask(mySegmentsStorage, otherSegment, mEventsManager);
        ArgumentCaptor<List<String>> segmentsCaptor = ArgumentCaptor.forClass(List.class);

        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, never()).set(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_REMOVAL, result.getTaskType());
    }

    @Test
    public void storageException() {
        
        mTask = new MySegmentsRemovalTask(mySegmentsStorage, mSegmentToRemove, mEventsManager);
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(any());

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void nullParameter() {

        mTask = new MySegmentsRemovalTask(mySegmentsStorage, null, mEventsManager);

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mySegmentsStorage);
    }

    private boolean isSegmentRemoved(List<String> segments, String segment) {
        return !(new HashSet<>(segments).contains(segment));
    }
}
