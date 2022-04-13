package io.split.android.client.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
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
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    @Mock
    private BackoffCounter mBackoffCounter;

    private SplitsSyncHelper mSplitsSyncHelper;

    private final Map<String, Object> mDefaultParams = new HashMap<>();
    private final Map<String, Object> mSecondFetchParams = new HashMap<>();

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mDefaultParams.clear();
        mDefaultParams.put("since", -1L);
        mSecondFetchParams.clear();
        mSecondFetchParams.put("since", 1506703262916L);
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor, mTelemetryRuntimeProducer, mBackoffCounter);
        loadSplitChanges();
    }

    @Test
    public void correctSyncExecution() throws HttpFetcherException {
        // On correct execution without having clear param
        // should execute fetcher, update storage and avoid clearing splits cache
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);
        SplitChange secondSplitChange = mSplitChange;
        secondSplitChange.since = mSplitChange.till;
        when(mSplitsFetcher.execute(mSecondFetchParams, null)).thenReturn(secondSplitChange);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, false, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        verify(mSplitsStorage, never()).clear();
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void correctSyncExecutionNoCache() throws HttpFetcherException {
        // On correct execution without having clear param
        // should execute fetcher, update storage and avoid clearing splits cache

        Map<String, String> headers = new HashMap<>();
        headers.put(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER, SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE);
        when(mSplitsFetcher.execute(mDefaultParams, headers)).thenReturn(mSplitChange);
        SplitChange secondSplitChange = mSplitChange;
        secondSplitChange.since = mSplitChange.till;
        when(mSplitsFetcher.execute(mSecondFetchParams, null)).thenReturn(secondSplitChange);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, false, true);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, headers);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
        verify(mSplitsStorage, never()).clear();
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void fetcherSyncException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null))
                .thenThrow(HttpFetcherException.class);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, never()).update(any());
        verify(mSplitsStorage, never()).clear();
        verify(mSplitChangeProcessor, never()).process(mSplitChange);
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void shouldClearStorageAfterFetch() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mSplitChange);
        SplitChange secondSplitChange = mSplitChange;
        secondSplitChange.since = mSplitChange.till;
        when(mSplitsFetcher.execute(mSecondFetchParams, null)).thenReturn(secondSplitChange);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
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
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        mSplitsSyncHelper.sync(-1, true, false);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.SPLITS, 500);
    }

    @Test
    public void performSplitsFetchUntilSinceEqualsTill() throws HttpFetcherException {
        SplitChange firstSplitChange = getSplitChange(-1, 2);
        SplitChange secondSplitChange = getSplitChange(2, 3);
        SplitChange thirdSplitChange = getSplitChange(3, 3);
        Map<String, Object> firstParams = getSinceParams(-1L);
        Map<String, Object> secondParams = getSinceParams(2L);
        Map<String, Object> thirdParams = getSinceParams(3L);

        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L);

        when(mSplitsFetcher.execute(eq(firstParams), any())).thenReturn(firstSplitChange);
        when(mSplitsFetcher.execute(eq(secondParams), any())).thenReturn(secondSplitChange);
        when(mSplitsFetcher.execute(eq(thirdParams), any())).thenReturn(thirdSplitChange);

        mSplitsSyncHelper.sync(3);

        verify(mSplitsStorage, times(3)).getTill();
        verify(mSplitsFetcher).execute(eq(firstParams), any());
        verify(mSplitsFetcher).execute(eq(secondParams), any());
        verify(mSplitsFetcher).execute(eq(thirdParams), any());
    }

    @Test
    public void performSplitFetchUntilStoredChangeNumberIsGreaterThanRequested() throws HttpFetcherException {
        SplitChange firstSplitChange = getSplitChange(-1, 2);
        SplitChange secondSplitChange = getSplitChange(2, 4);
        Map<String, Object> firstParams = getSinceParams(-1L);
        Map<String, Object> secondParams = getSinceParams(2L);

        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        when(mSplitsFetcher.execute(eq(firstParams), any())).thenReturn(firstSplitChange);
        when(mSplitsFetcher.execute(eq(secondParams), any())).thenReturn(secondSplitChange);

        mSplitsSyncHelper.sync(3);

        verify(mSplitsStorage, times(3)).getTill();
        verify(mSplitsFetcher).execute(eq(firstParams), any());
        verify(mSplitsFetcher).execute(eq(secondParams), any());
    }

    @Test
    public void syncWithClearBeforeUpdateOnlyClearsStorageOnce() {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(3, true, false);

        verify(mSplitsStorage).clear();
    }

    @Test
    public void syncWithoutClearBeforeUpdateDoesNotClearStorage() {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(3, false, false);

        verify(mSplitsStorage, never()).clear();
    }

    @Test
    public void cdnIsBypassedWhenNeeded() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L);
        when(mSplitsFetcher.execute(anyMap(), any())).thenReturn(
                getSplitChange(-1, 2),
                getSplitChange(2, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(4, 4)
        );

        mSplitsSyncHelper.sync(4);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-cache");
        Map<String, Object> firstParams = getSinceParams(-1L);
        Map<String, Object> secondParams = getSinceParams(2L);
        Map<String, Object> thirdParams = getSinceParams(3L);
        Map<String, Object> bypassedParams = getSinceParams(3L);
        bypassedParams.put("till", 3L);

        verify(mSplitsFetcher).execute(firstParams, headers);
        verify(mSplitsFetcher).execute(secondParams, headers);
        verify(mSplitsFetcher, times(10)).execute(thirdParams, headers);
        verify(mSplitsFetcher).execute(bypassedParams, headers);
    }

    @Test
    public void backoffIsAppliedWhenRetryingSplits() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L, 3L, 4L);
        when(mSplitsFetcher.execute(anyMap(), any())).thenReturn(
                getSplitChange(-1, 2),
                getSplitChange(2, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 3),
                getSplitChange(3, 4),
                getSplitChange(4, 4)
        );

        mSplitsSyncHelper.sync(4);

        verify(mBackoffCounter).resetCounter();
        verify(mBackoffCounter, times(2)).getNextRetryTime();
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }

    private Map<String, Object> getSinceParams(long since) {
        Map<String, Object> params = new HashMap<>();
        params.put("since", since);

        return params;
    }

    private SplitChange getSplitChange(int since, int till) {
        SplitChange splitChange = new SplitChange();
        splitChange.since = since;
        splitChange.till = till;
        splitChange.splits = new ArrayList<>();

        return splitChange;
    }
}
