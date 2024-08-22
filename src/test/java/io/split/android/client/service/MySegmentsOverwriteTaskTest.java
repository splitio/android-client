package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTaskConfig;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

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
        SegmentsChange segments = dummySegments();
        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, segments, mEventsManager, MySegmentsOverwriteTaskConfig.getForMySegments());
        SplitTaskExecutionInfo result = mTask.execute();

        verify(mySegmentsStorage, times(1)).set(argThat(argument -> argument.getSegments().size() == 3 &&
                argument.getChangeNumber() == -1L));

        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.MY_SEGMENTS_OVERWRITE, result.getTaskType());
    }

    @Test
    public void storageException() {
        SegmentsChange segments = dummySegments();
        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, segments, mEventsManager, MySegmentsOverwriteTaskConfig.getForMySegments());
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(argThat(argument -> argument.getSegments().size() == 3 &&
                argument.getChangeNumber() == -1L));

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void nullParameter() {

        mTask = new MySegmentsOverwriteTask(mySegmentsStorage, null, mEventsManager, MySegmentsOverwriteTaskConfig.getForMySegments());

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mySegmentsStorage);
    }

    private SegmentsChange dummySegments() {
        Set<String> segments = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            segments.add("segment" + i);
        }
        return SegmentsChange.create(segments, -1L);
    }
}
