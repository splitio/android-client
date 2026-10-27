package io.split.android.client.service.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import android.util.Log;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.logger.LogPrinterStub;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class LoadSplitsTaskTest {

    private SplitsStorage mSplitsStorage;
    private LoadSplitsTask mLoadSplitsTask;
    private LogPrinterStub mLogPrinter;

    @Before
    public void setUp() {
        mSplitsStorage = mock(SplitsStorage.class);
        mLogPrinter = new LogPrinterStub();
        Logger.instance().setPrinter(mLogPrinter);
    }

    private List<String> verboseMessages() {
        return new ArrayList<>(mLogPrinter.getLoggedMessages().get(Log.VERBOSE));
    }

    @After
    public void tearDown() {
        Logger.instance().setLevel(SplitLogLevel.NONE);
    }

    @Test
    public void successPathLogsOneVerboseTimingMessage() {
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);
        when(mSplitsStorage.getTill()).thenReturn(1L);

        SplitTaskExecutionInfo result = new LoadSplitsTask(mSplitsStorage, null, null).execute();

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        List<String> verbose = verboseMessages();
        assertEquals(1, verbose.size());
        assertTrue(verbose.get(0).contains("LoadSplitsTask"));
    }

    @Test
    public void errorPathLogsOneVerboseTimingMessage() {
        Logger.instance().setLevel(SplitLogLevel.VERBOSE);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = new LoadSplitsTask(mSplitsStorage, null, null).execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        List<String> verbose = verboseMessages();
        assertEquals(1, verbose.size());
        assertTrue(verbose.get(0).contains("LoadSplitsTask"));
    }

    @Test
    public void nothingIsLoggedWhenLevelIsNone() {
        Logger.instance().setLevel(SplitLogLevel.NONE);
        when(mSplitsStorage.getTill()).thenReturn(1L);

        new LoadSplitsTask(mSplitsStorage, null, null).execute();
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        new LoadSplitsTask(mSplitsStorage, null, null).execute();

        assertEquals(0, verboseMessages().size());
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
