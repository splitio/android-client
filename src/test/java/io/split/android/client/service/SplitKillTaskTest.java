package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SplitKillTaskTest {

    SplitsStorage mSplitsStorage;

    SplitKillTask mTask;


    @Before
    public void setup() {
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mTask = new SplitKillTask( mSplitsStorage);
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        Split split = new Split();
        mTask.setParam(split);

        SplitTaskExecutionInfo result = mTask.execute();
        verify(mSplitsStorage, times(1)).updateWithoutChecks(split);
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.SPLIT_KILL, result.getTaskType());
    }

    @Test
    public void nullParam() throws HttpFetcherException {
        mTask.setParam(null);
        SplitTaskExecutionInfo result = mTask.execute();
        verify(mSplitsStorage, never()).updateWithoutChecks(any());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        Split split = new Split();
        mTask.setParam(split);
        doThrow(NullPointerException.class).when(mSplitsStorage).updateWithoutChecks(split);

        SplitTaskExecutionInfo result = mTask.execute();
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mSplitsStorage);
    }
}
