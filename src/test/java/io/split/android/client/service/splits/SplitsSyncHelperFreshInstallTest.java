package io.split.android.client.service.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import io.split.android.client.dtos.Excluded;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.RuleBasedSegmentChange;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;

/**
 * Tests for fresh install prefetch functionality in SplitsSyncHelper.
 */
public class SplitsSyncHelperFreshInstallTest {

    @Mock
    private HttpFetcher<TargetingRulesChange> mSplitFetcher;

    private TargetingRulesCache mTargetingRulesCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mTargetingRulesCache = new TargetingRulesCache();
    }

    @Test
    public void fetchForFreshInstallCacheSuccessfullyFetchesAndCaches() throws Exception {
        TargetingRulesChange expectedChange = createTestTargetingRulesChange();
        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(expectedChange);

        SplitsSyncHelper.fetchForFreshInstallCache("1.0", mSplitFetcher, mTargetingRulesCache);

        verify(mSplitFetcher, times(1)).execute(anyMap(), any());
        assertNotNull("Cache should contain prefetched data", mTargetingRulesCache.getAndConsume());
    }

    @Test
    public void fetchForFreshInstallCacheUsesCorrectParameters() throws Exception {
        TargetingRulesChange expectedChange = createTestTargetingRulesChange();
        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(expectedChange);
        String flagsSpec = "1.3";

        SplitsSyncHelper.fetchForFreshInstallCache(flagsSpec, mSplitFetcher, mTargetingRulesCache);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSplitFetcher).execute(paramsCaptor.capture(), any());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("since parameter should be -1 for fresh install", -1, params.get("since"));
        assertEquals("rbSince parameter should be -1 for fresh install", -1, params.get("rbSince"));
        assertEquals("flagsSpec should be included", flagsSpec, params.get(ServiceConstants.FLAGS_SPEC_PARAM));
    }

    @Test
    public void fetchForFreshInstallCacheWithNullFlagsSpec() throws Exception {
        TargetingRulesChange expectedChange = createTestTargetingRulesChange();
        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(expectedChange);

        SplitsSyncHelper.fetchForFreshInstallCache(null, mSplitFetcher, mTargetingRulesCache);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSplitFetcher).execute(paramsCaptor.capture(), any());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("since parameter should be -1", -1, params.get("since"));
        assertEquals("rbSince parameter should be -1", -1, params.get("rbSince"));
        assertEquals("Should have exactly 2 parameters when flagsSpec is null", 2, params.size());
    }

    @Test
    public void fetchForFreshInstallCacheWithEmptyFlagsSpec() throws Exception {
        TargetingRulesChange expectedChange = createTestTargetingRulesChange();
        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(expectedChange);

        SplitsSyncHelper.fetchForFreshInstallCache("  ", mSplitFetcher, mTargetingRulesCache);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSplitFetcher).execute(paramsCaptor.capture(), any());

        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("Should have exactly 2 parameters when flagsSpec is empty", 2, params.size());
    }

    @Test(expected = HttpFetcherException.class)
    public void fetchForFreshInstallCacheThrowsHttpFetcherException() throws Exception {
        when(mSplitFetcher.execute(anyMap(), any()))
                .thenThrow(new HttpFetcherException("test_path", "Network error", 500));

        SplitsSyncHelper.fetchForFreshInstallCache("1.0", mSplitFetcher, mTargetingRulesCache);
    }

    @Test
    public void fetchForFreshInstallCacheHandlesGenericException() throws Exception {
        when(mSplitFetcher.execute(anyMap(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        try {
            SplitsSyncHelper.fetchForFreshInstallCache("1.0", mSplitFetcher, mTargetingRulesCache);
        } catch (Exception e) {
            throw new AssertionError("Should not throw exception for generic errors", e);
        }

        assertNull("Cache should be empty after error", mTargetingRulesCache.getAndConsume());
    }

    @Test
    public void fetchForFreshInstallCacheStoresDataInCache() throws Exception {
        Split testSplit = new Split();
        testSplit.name = "test_feature";
        testSplit.status = Status.ACTIVE;
        testSplit.changeNumber = 1234L;

        SplitChange splitChange = SplitChange.create(-1, 1234L, Collections.singletonList(testSplit));

        RuleBasedSegment testSegment = new RuleBasedSegment(
                "test_segment",
                "user",
                1234L,
                Status.ACTIVE,
                new ArrayList<>(),
                new Excluded()
        );

        RuleBasedSegmentChange rbsChange = RuleBasedSegmentChange.create(-1, 1234L, Collections.singletonList(testSegment));
        TargetingRulesChange targetingRulesChange = TargetingRulesChange.create(splitChange, rbsChange);

        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(targetingRulesChange);

        SplitsSyncHelper.fetchForFreshInstallCache("1.0", mSplitFetcher, mTargetingRulesCache);

        TargetingRulesChange cachedData = mTargetingRulesCache.getAndConsume();
        assertNotNull("Cache should contain data", cachedData);
        assertEquals("Cached split change till should match", 1234L, cachedData.getFeatureFlagsChange().till);
        assertEquals("Cached RBS change till should match", 1234L, cachedData.getRuleBasedSegmentsChange().getTill());
    }

    @Test
    public void fetchForFreshInstallCacheWithNoCacheHeaders() throws Exception {
        TargetingRulesChange expectedChange = createTestTargetingRulesChange();
        when(mSplitFetcher.execute(anyMap(), any())).thenReturn(expectedChange);

        SplitsSyncHelper.fetchForFreshInstallCache("1.0", mSplitFetcher, mTargetingRulesCache);

        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSplitFetcher).execute(anyMap(), headersCaptor.capture());

        Map<String, String> headers = headersCaptor.getValue();
        assertNotNull("Headers should be provided for fresh fetch", headers);
    }

    private TargetingRulesChange createTestTargetingRulesChange() {
        Split testSplit = new Split();
        testSplit.name = "test_split";
        testSplit.status = Status.ACTIVE;
        testSplit.changeNumber = 1000L;

        SplitChange splitChange = SplitChange.create(
                -1,
                1000L,
                Collections.singletonList(testSplit)
        );

        RuleBasedSegment testSegment = new RuleBasedSegment(
                "test_segment",
                "user",
                1000L,
                Status.ACTIVE,
                new ArrayList<>(),
                new Excluded()
        );

        RuleBasedSegmentChange ruleBasedSegmentChange = RuleBasedSegmentChange.create(
                -1,
                1000L,
                Collections.singletonList(testSegment)
        );

        return TargetingRulesChange.create(splitChange, ruleBasedSegmentChange);
    }
}
