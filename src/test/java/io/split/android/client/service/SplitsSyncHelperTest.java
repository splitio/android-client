package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    private AutoCloseable mAutoCloseable;

    @Before
    public void setup() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        mDefaultParams.clear();
        mDefaultParams.put("s", "1.1");
        mDefaultParams.put("since", -1L);
        mSecondFetchParams.clear();
        mSecondFetchParams.put("since", 1506703262916L);
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor, mTelemetryRuntimeProducer, mBackoffCounter, "1.1");
        loadSplitChanges();
    }

    @After
    public void tearDown() {
        try {
            mAutoCloseable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        mSplitsSyncHelper.sync(-1, true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        mSplitsSyncHelper.sync(3, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        mSplitsSyncHelper.sync(3, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage, times(3)).getTill();
        verify(mSplitsFetcher).execute(eq(firstParams), any());
        verify(mSplitsFetcher).execute(eq(secondParams), any());
    }

    @Test
    public void syncWithClearBeforeUpdateOnlyClearsStorageOnce() {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(3, true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage).clear();
    }

    @Test
    public void syncWithoutClearBeforeUpdateDoesNotClearStorage() {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(3, false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        mSplitsSyncHelper.sync(4, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

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

        mSplitsSyncHelper.sync(4, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mBackoffCounter).resetCounter();
        verify(mBackoffCounter, times(2)).getNextRetryTime();
    }

    @Test
    public void replaceTillWhenFilterHasChanged() throws HttpFetcherException {
        mSplitsSyncHelper.sync(14829471, true, true, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        Map<String, Object> params = new HashMap<>();
        params.put("s", "1.1");
        params.put("since", -1L);
        verify(mSplitsFetcher).execute(eq(params), eq(null));
        verifyNoMoreInteractions(mSplitsFetcher);
    }

    @Test
    public void returnTaskInfoToDoNotRetryWhenHttpFetcherExceptionStatusCodeIs414() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 414));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void doNotRetryFlagIsNullWhenFetcherExceptionStatusCodeIsNot414() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 500));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void returnTaskInfoToDoNotRetryWhenHttpFetcherExceptionStatusCodeIs9009() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 9009));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void doNotRetryFlagIsNullWhenFetcherExceptionStatusCodeIsNot9009() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 500));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(-1, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void defaultQueryParamOrderIsCorrect() throws HttpFetcherException {
        mSplitsSyncHelper.sync(100, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher).execute(argThat(new ArgumentMatcher<Map<String, Object>>() {
            @Override
            public boolean matches(Map<String, Object> argument) {
                List<String> keys = new ArrayList<>(argument.keySet());
                return keys.get(0).equals("s") &&
                        keys.get(1).equals("since");
            }
        }), any());
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }

    private Map<String, Object> getSinceParams(long since) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("s", "1.1");
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
