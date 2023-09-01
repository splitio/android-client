package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.storage.splits.SplitsStorage;

public class LoadSplitsTaskTest {

    private SplitsStorage mSplitsStorage;
    private LoadSplitsTask mLoadSplitsTask;

    @Before
    public void setUp() {
        mSplitsStorage = mock(SplitsStorage.class);
    }

    @Test
    public void resultIsErrorWhenQueryStringHasChanged() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("previous");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "new");

        SplitTaskExecutionInfo info = mLoadSplitsTask.execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
        assertEquals(SplitTaskType.LOAD_LOCAL_SPLITS, info.getTaskType());
    }

    @Test
    public void resultIsSuccessWhenQueryStringIsSame() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("previous");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "previous");

        SplitTaskExecutionInfo info = mLoadSplitsTask.execute();

        assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());
        assertEquals(SplitTaskType.LOAD_LOCAL_SPLITS, info.getTaskType());
    }

    @Test
    public void loadLocalIsCalledOnStorageWhenExecutingTask() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null);

        mLoadSplitsTask.execute();

        verify(mSplitsStorage).loadLocal();
    }

    @Test
    public void resultIsErrorWhenTillIsNegativeOne() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null);

        SplitTaskExecutionInfo info = mLoadSplitsTask.execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
        assertEquals(SplitTaskType.LOAD_LOCAL_SPLITS, info.getTaskType());
    }

    @Test
    public void clearIsNotCalledWhenTillIsNegativeOne() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null);

        mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).clear();
    }

    @Test
    public void clearIsCalledOnStorageWhenQueryStringsDiffer() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("previous");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "new");

        mLoadSplitsTask.execute();

        verify(mSplitsStorage).clear();
    }

    @Test
    public void clearIsNotCalledOnStorageWhenQueryStringsAreEquallyNull() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(null);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null);

        mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).clear();
    }

    @Test
    public void clearIsNotCalledOnStorageWhenQueryStringsAreEqual() {
        when(mSplitsStorage.getTill()).thenReturn(123456677L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "");

        mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).clear();
    }
}
