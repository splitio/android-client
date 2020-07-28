package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitsSyncHelperTest {

    HttpFetcher<SplitChange> mSplitsFetcher;
    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitChangeProcessor mSplitChangeProcessor;

    SplitsSyncHelper mSplitsSyncHelper;

    Map<String, Object> mDefaultParams = new HashMap<>();


    @Before
    public void setup() {
        mDefaultParams.clear();
        mDefaultParams.put("since", -1L);
        mSplitsFetcher = (HttpFetcher<SplitChange>) Mockito.mock(HttpFetcher.class);
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mSplitChangeProcessor = Mockito.spy(SplitChangeProcessor.class);
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor);
        loadSplitChanges();
    }

    @Test
    public void correctSyncExecution() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams)).thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(mDefaultParams);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void fetcherSyncException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams))
                .thenThrow(HttpFetcherException.class);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(mDefaultParams);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams);
        verify(mSplitsStorage, never()).update(any());
        verify(mSplitChangeProcessor, never()).process(mSplitChange);
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void correctSyncRetryExecution() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams)).thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.syncUntilSuccess(mDefaultParams);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void fetcherSyncRetryException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams))
                .thenThrow(HttpFetcherException.class)
                .thenThrow(HttpFetcherException.class)
                .thenThrow(HttpFetcherException.class)
                .thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.syncUntilSuccess(mDefaultParams);

        verify(mSplitsFetcher, times(4)).execute(mDefaultParams);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams)).thenReturn(mSplitChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class));

        SplitTaskExecutionInfo result = mSplitsSyncHelper.syncUntilSuccess(mDefaultParams);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @After
    public void tearDown() {
        reset(mSplitsFetcher);
        reset(mSplitsStorage);
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }
}
