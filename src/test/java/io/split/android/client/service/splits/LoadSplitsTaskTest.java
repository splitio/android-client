package io.split.android.client.service.splits;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.storage.splits.SplitsStorage;

public class LoadSplitsTaskTest {

    private SplitsStorage mSplitsStorage;
    private LoadSplitsTask mLoadSplitsTask;

    @Before
    public void setUp() {
        mSplitsStorage = mock(SplitsStorage.class);
    }

    @Test
    public void executeCallsLoadLocal() {
        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null, null);
        mLoadSplitsTask.execute();
        verify(mSplitsStorage).loadLocal();
    }

    @Test
    public void resultIsSuccessWhenTillFromStorageIsNotInitialAndFilterAndSpecHaveNotChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");
        when(mSplitsStorage.getTill()).thenReturn(1L);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "filter", "spec");
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void resultIsErrorWhenTillIsInitial() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsStorage.getFlagsSpec()).thenReturn(null);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(null);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, null, null);
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void cacheIsNotClearedWhenFilterNorSpecHaveChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");
        when(mSplitsStorage.getTill()).thenReturn(1L);

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "filter", "spec");
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).clear();
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void cacheIsClearedWhenFilterHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "newFilter", "spec");
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        verify(mSplitsStorage).clear();
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void cacheIsClearedWhenSpecHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "filter", "newSpec");
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        verify(mSplitsStorage).clear();
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void cacheIsClearedWhenBothFilterAndSpecHaveChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "newFilter", "newSpec");
        SplitTaskExecutionInfo result = mLoadSplitsTask.execute();

        verify(mSplitsStorage).clear();
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageQueryStringIsUpdatedWhenItHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "newFilter", "spec");
        mLoadSplitsTask.execute();

        verify(mSplitsStorage).updateSplitsFilterQueryString("newFilter");
    }

    @Test
    public void storageSpecIsUpdatedWhenItHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "filter", "newSpec");
        mLoadSplitsTask.execute();

        verify(mSplitsStorage).updateFlagsSpec("newSpec");
    }

    @Test
    public void storageQueryStringIsNotUpdatedWhenOnlySpecHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "filter", "newSpec");
        mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).updateSplitsFilterQueryString("filter");
    }

    @Test
    public void storageSpecIsNotUpdatedWhenOnlyFilterHasChanged() {
        when(mSplitsStorage.getFlagsSpec()).thenReturn("spec");
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filter");

        mLoadSplitsTask = new LoadSplitsTask(mSplitsStorage, "newFilter", "spec");
        mLoadSplitsTask.execute();

        verify(mSplitsStorage, times(0)).updateFlagsSpec("spec");
    }
}
