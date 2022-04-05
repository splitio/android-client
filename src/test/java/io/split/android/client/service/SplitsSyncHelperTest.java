package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitsSyncHelperTest {

    @Mock
    HttpFetcher<SplitChange> mSplitsFetcher;
    @Mock
    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    @Spy
    SplitChangeProcessor mSplitChangeProcessor;
    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    SplitsSyncHelper mSplitsSyncHelper;

    Map<String, Object> mDefaultParams = new HashMap<>();


    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mDefaultParams.clear();
        mDefaultParams.put("since", -1L);
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor, mTelemetryRuntimeProducer);
        loadSplitChanges();
    }

    @Test
    public void correctSyncExecution() throws HttpFetcherException {
        // On correct execution without having clear param
        // should execute fetcher, update storage and avoid clearing splits cache
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, false, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        verify(mSplitsStorage, never()).clear();
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void correctSyncExecutionNoCache() throws HttpFetcherException {
        // On correct execution without having clear param
        // should execute fetcher, update storage and avoid clearing splits cache

        Map<String, String> headers = new HashMap<>();
        headers.put(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER, SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE);
        when(mSplitsFetcher.execute(mDefaultParams, headers)).thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, false, true);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, headers);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        verify(mSplitsStorage, never()).clear();
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void fetcherSyncException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null))
                .thenThrow(HttpFetcherException.class);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, never()).update(any());
        verify(mSplitsStorage, never()).clear();
        verify(mSplitChangeProcessor, never()).process(mSplitChange);
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class));

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);

        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void shouldClearStorageAfterFetch() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);

        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void cacheExpired() throws HttpFetcherException {

        // change number > -1 should clear cache
        // when cache expired

        long cacheExpInSeconds = 10000;
        long updateTimestamp = System.currentTimeMillis() / 1000 - cacheExpInSeconds - 1;
        boolean expired = mSplitsSyncHelper.cacheHasExpired(100, updateTimestamp, cacheExpInSeconds);

        Assert.assertTrue(expired);
    }

    @Test
    public void cacheNotExpired() throws HttpFetcherException {

        // change number > -1 should clear cache
        // only when cache expired

        long cacheExpInSeconds = 10000;
        long updateTimestamp = System.currentTimeMillis() / 1000 - cacheExpInSeconds + 1000;
        boolean expired = mSplitsSyncHelper.cacheHasExpired(100, updateTimestamp, cacheExpInSeconds);

        Assert.assertFalse(expired);
    }

    @Test
    public void cacheExpiredButChangeNumber() throws HttpFetcherException {

        // change number = -1 means no previous cache available
        // so, should no clear cache
        // even if it's expired

        long cacheExpInSeconds = 10000;
        long updateTimestamp = System.currentTimeMillis() / 1000 - cacheExpInSeconds - 1000;
        boolean expired = mSplitsSyncHelper.cacheHasExpired(-1, updateTimestamp, cacheExpInSeconds);

        Assert.assertFalse(expired);
    }

    @Test
    public void errorIsRecordedInTelemetry() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null))
                .thenThrow(new HttpFetcherException("error", "error", 500));

        mSplitsSyncHelper.sync(-1, true, false);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.SPLITS, 500);
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
