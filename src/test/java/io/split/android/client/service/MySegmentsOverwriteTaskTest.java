package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MySegmentsOverwriteTaskTest {

    @Mock
    MySegmentsStorage mySegmentsStorage;

    @Mock
    SplitEventsManager mEventsManager;

    MySegmentsOverwriteTask mTask;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        List<String> segments = dummySegments();
        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, segments, mEventsManager);
        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(any());

        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_OVERWRITE, result.getTaskType());
    }

    @Test
    public void storageException() {
        List<String> segments = dummySegments();
        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, segments, mEventsManager);
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(segments);

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void nullParameter() {

        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, null, mEventsManager);

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mySegmentsStorage);
    }

    private List<String> dummySegments() {
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            segments.add("segment" + i);
        }
        return segments;
    }
}
