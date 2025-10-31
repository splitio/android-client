package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.RuleBasedSegmentChange;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.service.rules.RuleBasedSegmentChangeProcessor;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageImplTest;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

public class SplitsSyncHelperTest {

    @Mock
    HttpFetcher<TargetingRulesChange> mSplitsFetcher;
    @Mock
    SplitsStorage mSplitsStorage;
    TargetingRulesChange mTargetingRulesChange = TargetingRulesChange.create(SplitChange.create(-1, 1506703262916L, Collections.emptyList()), RuleBasedSegmentChange.create(-1, 262325L, Collections.singletonList(RuleBasedSegmentStorageImplTest.createRuleBasedSegment("rbs"))));
    @Spy
    SplitChangeProcessor mSplitChangeProcessor;
    @Spy
    RuleBasedSegmentChangeProcessor mRuleBasedSegmentChangeProcessor;
    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    @Mock
    private BackoffCounter mBackoffCounter;
    @Mock
    private RuleBasedSegmentStorageProducer mRuleBasedSegmentStorageProducer;
    @Mock
    private GeneralInfoStorage mGeneralInfoStorage;

    private SplitsSyncHelper mSplitsSyncHelper;

    private Map<String, Object> mDefaultParams = new HashMap<>();
    private Map<String, Object> mSecondFetchParams = new HashMap<>();
    private AutoCloseable mAutoCloseable;

    @Before
    public void setup() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        mDefaultParams.clear();
        mDefaultParams = getSinceParams(-1, -1);
        mSecondFetchParams = getSinceParams(1506703262916L, 262325L);
        when(mRuleBasedSegmentStorageProducer.getChangeNumber()).thenReturn(-1L).thenReturn(262325L);
        // Use a short proxy check interval for all tests
        mSplitsSyncHelper = new SplitsSyncHelper(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor, mRuleBasedSegmentChangeProcessor, mRuleBasedSegmentStorageProducer, mGeneralInfoStorage, mTelemetryRuntimeProducer, mBackoffCounter, "1.3", false, 1L, null);
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
        SplitChange secondSplitChange = mTargetingRulesChange.getFeatureFlagsChange();
        secondSplitChange.since = mTargetingRulesChange.getFeatureFlagsChange().till;
        when(mSplitsFetcher.execute(any(), any()))
                .thenReturn(TargetingRulesChange.create(secondSplitChange, RuleBasedSegmentChange.create(262325L, 262325L, Collections.emptyList())));
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mRuleBasedSegmentStorageProducer.getChangeNumber()).thenReturn(-1L).thenReturn(262325L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any(), any());
        verify(mSplitChangeProcessor, times(1)).process(mTargetingRulesChange.getFeatureFlagsChange());
        verify(mRuleBasedSegmentChangeProcessor).process((List<RuleBasedSegment>) any(), anyLong());
        verify(mRuleBasedSegmentStorageProducer, times(1)).update(any(), any(), eq(262325L), any());
        verify(mSplitsStorage, never()).clear();
        verify(mRuleBasedSegmentStorageProducer, never()).clear();
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void correctSyncExecutionNoCache() throws HttpFetcherException {
        // On correct execution without having clear param
        // should execute fetcher, update storage and avoid clearing splits cache

        Map<String, String> headers = new HashMap<>();
        headers.put(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER, SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE);
        SplitChange secondSplitChange = mTargetingRulesChange.getFeatureFlagsChange();
        secondSplitChange.since = mTargetingRulesChange.getFeatureFlagsChange().till;
        when(mSplitsFetcher.execute(any(), any()))
                .thenReturn(TargetingRulesChange.create(secondSplitChange, RuleBasedSegmentChange.create(262325L, 262325L, Collections.emptyList())));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, headers);
        verify(mSplitsStorage, times(1)).update(any(), any());
        verify(mSplitChangeProcessor, times(1)).process(mTargetingRulesChange.getFeatureFlagsChange());
        verify(mSplitsStorage, never()).clear();
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void fetcherSyncException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null))
                .thenThrow(HttpFetcherException.class);
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, never()).update(any(), any());
        verify(mSplitsStorage, never()).clear();
        verify(mSplitChangeProcessor, never()).process(mTargetingRulesChange.getFeatureFlagsChange());
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mTargetingRulesChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class), any());
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any(), any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mTargetingRulesChange.getFeatureFlagsChange());

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void shouldClearStorageAfterFetch() throws HttpFetcherException {
        SplitChange secondSplitChange = mTargetingRulesChange.getFeatureFlagsChange();
        secondSplitChange.since = mTargetingRulesChange.getFeatureFlagsChange().till;
        when(mSplitsFetcher.execute(any(), any()))
                .thenReturn(TargetingRulesChange.create(secondSplitChange, RuleBasedSegmentChange.create(262325L, 262325L, Collections.emptyList())));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher, times(1)).execute(mDefaultParams, null);
        verify(mSplitsStorage, times(1)).update(any(), any());
        verify(mSplitsStorage, times(1)).clear();
        verify(mSplitChangeProcessor, times(1)).process(mTargetingRulesChange.getFeatureFlagsChange());

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void errorIsRecordedInTelemetry() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null))
                .thenThrow(new HttpFetcherException("error", "error", 500));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.SPLITS, 500);
    }

    @Test
    public void performSplitsFetchUntilSinceEqualsTill() throws HttpFetcherException {
        TargetingRulesChange firstSplitChange = getSplitChange(-1, 2);
        TargetingRulesChange secondSplitChange = getSplitChange(2, 3);
        TargetingRulesChange thirdSplitChange = getSplitChange(3, 3);
        Map<String, Object> firstParams = getSinceParams(-1L, -1L);
        Map<String, Object> secondParams = getSinceParams(2L, 262325L);
        Map<String, Object> thirdParams = getSinceParams(3L, 262325L);

        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L);

        when(mSplitsFetcher.execute(eq(firstParams), any())).thenReturn(firstSplitChange);
        when(mSplitsFetcher.execute(eq(secondParams), any())).thenReturn(secondSplitChange);
        when(mSplitsFetcher.execute(eq(thirdParams), any())).thenReturn(thirdSplitChange);

        mSplitsSyncHelper.sync(getSinceChangeNumbers(3, -1), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage, times(3)).getTill();
        verify(mSplitsFetcher).execute(eq(firstParams), any());
        verify(mSplitsFetcher).execute(eq(secondParams), any());
        verify(mSplitsFetcher).execute(eq(thirdParams), any());
    }

    @Test
    public void performSplitFetchUntilStoredChangeNumberIsGreaterThanRequested() throws HttpFetcherException {
        TargetingRulesChange firstSplitChange = getSplitChange(-1, 2);
        TargetingRulesChange secondSplitChange = getSplitChange(2, 4);
        TargetingRulesChange thirdSplitChange = getSplitChange(4, 4);

        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        when(mSplitsFetcher.execute(any(), any())).thenReturn(firstSplitChange).thenReturn(secondSplitChange).thenReturn(thirdSplitChange);

        mSplitsSyncHelper.sync(getSinceChangeNumbers(3, -1), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage, times(3)).getTill();
        verify(mSplitsFetcher, times(3)).execute(any(), any());
    }

    @Test
    public void syncWithClearBeforeUpdateOnlyClearsStorageOnce() throws HttpFetcherException {
        when(mSplitsFetcher.execute(mDefaultParams, null)).thenReturn(mTargetingRulesChange);
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(getSinceChangeNumbers(3, -1), true, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage).clear();
    }

    @Test
    public void syncWithoutClearBeforeUpdateDoesNotClearStorage() {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 4L);

        mSplitsSyncHelper.sync(getSinceChangeNumbers(3, -1), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsStorage, never()).clear();
    }

    @Test
    public void cdnIsBypassedWhenNeeded() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L);
        when(mRuleBasedSegmentStorageProducer.getChangeNumber()).thenReturn(-1L);
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

        mSplitsSyncHelper.sync(getSinceChangeNumbers(4, -1), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-cache");
        Map<String, Object> firstParams = getSinceParams(-1L, -1L);
        Map<String, Object> secondParams = getSinceParams(2L, -1L);
        Map<String, Object> thirdParams = getSinceParams(3L, -1L);
        Map<String, Object> bypassedParams = getSinceParams(3L, -1L);
        bypassedParams.put("till", 3L);

        verify(mSplitsFetcher).execute(firstParams, headers);
        verify(mSplitsFetcher).execute(secondParams, headers);
        verify(mSplitsFetcher, times(10)).execute(thirdParams, headers);
        verify(mSplitsFetcher).execute(bypassedParams, headers);
    }

    @Test
    public void cdnIsBypassedWhenNeededWithRuleBasedSegments() throws HttpFetcherException {
        when(mRuleBasedSegmentStorageProducer.getChangeNumber()).thenReturn(-1L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L);
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(anyMap(), any())).thenReturn(
                getRuleBasedSegmentChange(-1, 2),
                getRuleBasedSegmentChange(2, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(3, 3),
                getRuleBasedSegmentChange(4, 4)
        );

        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, 4), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cache-Control", "no-cache");
        Map<String, Object> firstParams = getSinceParams(-1L, -1L);
        Map<String, Object> secondParams = getSinceParams(-1L, 2L);
        Map<String, Object> thirdParams = getSinceParams(-1L, 3L);
        Map<String, Object> bypassedParams = getSinceParams(-1L, 3L);
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

        mSplitsSyncHelper.sync(getSinceChangeNumbers(4, -1), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mBackoffCounter).resetCounter();
        verify(mBackoffCounter, times(2)).getNextRetryTime();
    }

    @Test
    public void replaceTillWhenFilterHasChanged() throws HttpFetcherException {
        mSplitsSyncHelper.sync(getSinceChangeNumbers(14829471, -1), true, true, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        Map<String, Object> params = new HashMap<>();
        params.put("s", "1.3");
        params.put("since", -1L);
        params.put("rbSince", -1L);
        verify(mSplitsFetcher).execute(eq(params), eq(null));
        verifyNoMoreInteractions(mSplitsFetcher);
    }

    @Test
    public void returnTaskInfoToDoNotRetryWhenHttpFetcherExceptionStatusCodeIs414() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 414));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void doNotRetryFlagIsNullWhenFetcherExceptionStatusCodeIsNot414() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 500));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void returnTaskInfoToDoNotRetryWhenHttpFetcherExceptionStatusCodeIs9009() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 9009));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertEquals(true, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void doNotRetryFlagIsNullWhenFetcherExceptionStatusCodeIsNot9009() throws HttpFetcherException {
        when(mSplitsFetcher.execute(eq(mDefaultParams), any()))
                .thenThrow(new HttpFetcherException("error", "error", 500));
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        SplitTaskExecutionInfo result = mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void defaultQueryParamOrderIsCorrect() throws HttpFetcherException {
        mSplitsSyncHelper.sync(getSinceChangeNumbers(100, -1), ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        verify(mSplitsFetcher).execute(argThat(new ArgumentMatcher<Map<String, Object>>() {
            @Override
            public boolean matches(Map<String, Object> argument) {
                List<String> keys = new ArrayList<>(argument.keySet());
                return keys.get(0).equals("s") &&
                        keys.get(1).equals("since");
            }
        }), any());
    }

    @Test
    public void proxyErrorTriggersFallbackAndOmitsRbSince() throws Exception {
        // Before first sync (no fallback)
        when(mGeneralInfoStorage.getLastProxyUpdateTimestamp()).thenReturn(0L);

        // Simulate proxy outdated error (400 with latest spec)
        when(mSplitsFetcher.execute(any(), any()))
                .thenThrow(new HttpFetcherException("Proxy outdated", "Proxy outdated", HttpStatus.INTERNAL_PROXY_OUTDATED.getCode())) // Use real status code
                .thenReturn(TargetingRulesChange.create(SplitChange.create(-1, 2, Collections.emptyList()), RuleBasedSegmentChange.create(-1, 2, Collections.emptyList())))
                .thenReturn(TargetingRulesChange.create(SplitChange.create(2, 2, Collections.emptyList()), RuleBasedSegmentChange.create(2, 2, Collections.emptyList())));
        when(mSplitsStorage.getTill()).thenReturn(-1L, -1L);

        // First sync triggers the proxy error and sets fallback mode
        try {
            mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        } catch (Exception ignored) {}

        // Now simulate fallback state persisted
        when(mGeneralInfoStorage.getLastProxyUpdateTimestamp()).thenReturn(System.currentTimeMillis());

        // Second sync, now in fallback mode, should use fallback spec and omit rbSince
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);

        // Capture and verify the params
        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSplitsFetcher, atLeastOnce()).execute(paramsCaptor.capture(), any());
        boolean foundFallback = false;
        for (Map params : paramsCaptor.getAllValues()) {
            System.out.println("Captured params: " + params);

            if ("1.2".equals(params.get("s")) && params.get("since") != null && !params.containsKey("rbSince")) {
                foundFallback = true;
                break;
            }
        }
        assertTrue("Expected a fallback call with s=1.2 and no rbSince", foundFallback);
    }

    @Test
    public void fallbackPersistsUntilIntervalElapses() throws Exception {
        // Simulate proxy outdated error
        long timestamp = System.currentTimeMillis();
        when(mGeneralInfoStorage.getLastProxyUpdateTimestamp()).thenReturn(timestamp);
        when(mSplitsFetcher.execute(any(), any()))
                .thenThrow(new HttpFetcherException("Proxy outdated", "Proxy outdated", HttpStatus.INTERNAL_PROXY_OUTDATED.getCode()))
                // First fallback fetch returns till=2, second fallback fetch returns till=2 (still not caught up),
                // third fallback fetch returns till=3 (caught up, loop can exit)
                .thenReturn(TargetingRulesChange.create(SplitChange.create(-1, 2, Collections.emptyList()), RuleBasedSegmentChange.create(-1, 2, Collections.emptyList())))
                .thenReturn(TargetingRulesChange.create(SplitChange.create(3, 3, Collections.emptyList()), RuleBasedSegmentChange.create(3, 3, Collections.emptyList())));
        // Simulate advancing change numbers for storage
        when(mSplitsStorage.getTill()).thenReturn(-1L, 2L, 3L);
        // Trigger fallback
        try { mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES); } catch (Exception ignored) {}
        // Simulate time NOT elapsed
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        verify(mSplitsFetcher, times(1)).execute(argThat(params ->
                "1.2".equals(params.get("s")) &&
                !params.containsKey("rbSince")
        ), any());
    }

    @Test
    public void generic400InFallbackDoesNotResetToNone() throws Exception {
        // Simulate proxy outdated error
        when(mSplitsFetcher.execute(any(), any()))
                .thenThrow(new HttpFetcherException("Proxy outdated", "Proxy outdated", HttpStatus.INTERNAL_PROXY_OUTDATED.getCode()))
                .thenThrow(new HttpFetcherException("Bad Request", "Bad Request", 400));
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        // Trigger fallback
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        // Next call gets a generic 400, should remain in fallback
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        verify(mSplitsFetcher, times(2)).execute(argThat(params ->
                "1.2".equals(params.get("s")) &&
                !params.containsKey("rbSince")
        ), any());
    }

    @Test
    public void successfulRecoveryReturnsToNormalSpec() throws Exception {
        // Simulate proxy outdated error, then recovery
        when(mSplitsFetcher.execute(any(), any()))
                .thenThrow(new HttpFetcherException("Proxy outdated", "Proxy outdated", HttpStatus.INTERNAL_PROXY_OUTDATED.getCode()))
                .thenReturn(TargetingRulesChange.create(SplitChange.create(-1, 2, Collections.emptyList()), RuleBasedSegmentChange.create(-1, 2, Collections.emptyList())))
                .thenReturn(TargetingRulesChange.create(SplitChange.create(2, 2, Collections.emptyList()), RuleBasedSegmentChange.create(2, 2, Collections.emptyList())));
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        // Trigger fallback
        try { mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES); } catch (Exception ignored) {}
        // Simulate interval elapsed
        when(mGeneralInfoStorage.getLastProxyUpdateTimestamp()).thenReturn(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
        mSplitsSyncHelper.sync(getSinceChangeNumbers(-1, -1L), false, false, ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES);
        // Next call should be with latest spec
        verify(mSplitsFetcher, times(1)).execute(argThat(params ->
                "1.3".equals(params.get("s")) &&
                params.containsKey("rbSince")
        ), any());
    }

    private static SplitsSyncHelper.SinceChangeNumbers getSinceChangeNumbers(int flagsSince, long rbsSince) {
        return new SplitsSyncHelper.SinceChangeNumbers(flagsSince, rbsSince);
    }

    private void loadSplitChanges() {
        if (mTargetingRulesChange == null) {
            FileHelper fileHelper = new FileHelper();
            mTargetingRulesChange = TargetingRulesChange.create(fileHelper.loadSplitChangeFromFile("split_changes_1.json"));
        }
    }

    private Map<String, Object> getSinceParams(long since, long rbSince) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("s", "1.3");
        params.put("since", since);
        params.put("rbSince", rbSince);

        return params;
    }

    private TargetingRulesChange getSplitChange(int since, int till) {
        SplitChange splitChange = new SplitChange();
        splitChange.since = since;
        splitChange.till = till;
        splitChange.splits = new ArrayList<>();

        return TargetingRulesChange.create(splitChange);
    }

    private TargetingRulesChange getRuleBasedSegmentChange(int since, int till) {
        RuleBasedSegmentChange ruleBasedSegmentChange = RuleBasedSegmentChange.create(since, till, new ArrayList<>());

        return TargetingRulesChange.create(SplitChange.create(10, 10, new ArrayList<>()), ruleBasedSegmentChange);
    }
}
