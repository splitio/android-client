package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.split.android.client.dtos.Split;
import io.split.android.client.events.SplitEventsManager;
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
import static org.mockito.Mockito.when;

public class SplitKillTaskTest {

    @Mock
    SplitEventsManager mEventsManager;

    SplitsStorage mSplitsStorage;

    SplitKillTask mTask;


    @Before
    public void setup() {
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        Split split = new Split();
        split.name = "split1";
        split.defaultTreatment = "off";
        split.changeNumber = 100;
        split.killed = false;
        when(mSplitsStorage.get(any())).thenReturn(split);

    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        Split split = new Split();
        split.name = "split1";
        split.defaultTreatment = "on";
        split.changeNumber = 1001;
        mTask = new SplitKillTask( mSplitsStorage, split, mEventsManager);

        when(mSplitsStorage.getTill()).thenReturn(1000L);

        SplitTaskExecutionInfo result = mTask.execute();
        ArgumentCaptor<Split> splitCaptor =
                ArgumentCaptor.forClass(Split.class);
        verify(mSplitsStorage, times(1)).updateWithoutChecks(splitCaptor.capture());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(SplitTaskType.SPLIT_KILL, result.getTaskType());
        Assert.assertEquals(split.name, splitCaptor.getValue().name);
        Assert.assertEquals(split.defaultTreatment, splitCaptor.getValue().defaultTreatment);
        Assert.assertEquals(split.changeNumber, splitCaptor.getValue().changeNumber);
        Assert.assertEquals(true, splitCaptor.getValue().killed);
    }

    @Test
    public void oldChangeNumber() throws HttpFetcherException {
        Split split = new Split();
        split.name = "split1";
        split.defaultTreatment = "on";
        split.changeNumber = 1001;
        mTask = new SplitKillTask( mSplitsStorage, split, mEventsManager);

        when(mSplitsStorage.getTill()).thenReturn(1002L);

        SplitTaskExecutionInfo result = mTask.execute();
        ArgumentCaptor<Split> splitCaptor =
                ArgumentCaptor.forClass(Split.class);
        verify(mSplitsStorage, never()).updateWithoutChecks(splitCaptor.capture());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void nullParam() throws HttpFetcherException {
        mTask = new SplitKillTask( mSplitsStorage, null, mEventsManager);
        SplitTaskExecutionInfo result = mTask.execute();
        verify(mSplitsStorage, never()).updateWithoutChecks(any());
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageException() {
        Split split = new Split();
        split.changeNumber = 2000L;
        when(mSplitsStorage.getTill()).thenReturn(1000L);
        mTask = new SplitKillTask( mSplitsStorage, split, mEventsManager);
        doThrow(NullPointerException.class).when(mSplitsStorage).updateWithoutChecks(any());
        SplitTaskExecutionInfo result = mTask.execute();
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mSplitsStorage);
    }
}
