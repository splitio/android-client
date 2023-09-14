package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;

public class TreatmentManagerWithFlagSetsTest {

    @Mock
    private Evaluator mEvaluator;
    @Mock
    private KeyValidator mKeyValidator;
    @Mock
    private SplitValidator mSplitValidator;
    @Mock
    private ImpressionListener mImpressionListener;
    @Mock
    private ListenableEventsManager mEventsManager;
    @Mock
    private AttributesManager mAttributesManager;
    @Mock
    private AttributesMerger mAttributesMerger;
    @Mock
    private TelemetryStorageProducer mTelemetryStorageProducer;
    @Mock
    private SplitsStorage mSplitsStorage;

    private FlagSetsFilter mFlagSetsFilter;
    private TreatmentManagerImpl mTreatmentManager;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);

        mFlagSetsFilter = new FlagSetsFilterImpl(new HashSet<>());
        when(mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)).thenReturn(true);

        initializeTreatmentManager();

        when(mEvaluator.getTreatment(anyString(), anyString(), eq("test_1"), anyMap()))
                .thenReturn(new EvaluationResult("result_1", "label"));
        when(mEvaluator.getTreatment(anyString(), anyString(), eq("test_2"), anyMap()))
                .thenReturn(new EvaluationResult("result_2", "label"));
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
    public void getTreatmentsByFlagSetDestroyedDoesNotUseEvaluator() {
        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, true);

        verify(mSplitsStorage).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetWithNoConfiguredSetsQueriesStorageAndUsesEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetWithNoConfiguredSetsInvalidSetDoesNotQueryStorageNorUseEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsByFlagSet("SET!", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetWithConfiguredSetsExistingSetQueriesStorageAndUsesEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsByFlagSet("set_2", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    private void initializeTreatmentManager() {
        mTreatmentManager = new TreatmentManagerImpl(
                "matching_key",
                "bucketing_key",
                mEvaluator,
                mKeyValidator,
                mSplitValidator,
                mImpressionListener,
                SplitClientConfig.builder().build().labelsEnabled(),
                mEventsManager,
                mAttributesManager,
                mAttributesMerger,
                mTelemetryStorageProducer,
                mFlagSetsFilter,
                mSplitsStorage);
    }

    @Test
    public void getTreatmentsByFlagSetReturnsCorrectFormat() {
        Set<String> mockNames = new HashSet<>();
        mockNames.add("test_1");
        mockNames.add("test_2");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(mockNames);
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));

        Map<String, String> result = mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        assertEquals(2, result.size());
        assertEquals("result_1", result.get("test_1"));
        assertEquals("result_2", result.get("test_2"));
    }

    @Test
    public void getTreatmentsByFlagSetRecordsTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(Collections.singleton("test_1"));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mTelemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_BY_FLAG_SET), anyLong());
    }

    ///
    @Test
    public void getTreatmentsByFlagSetsDestroyedDoesNotUseEvaluator() {
        mTreatmentManager.getTreatmentsByFlagSets(Collections.singletonList("set_1"), null, true);

        verify(mSplitsStorage).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsWithNoConfiguredSetsQueriesStorageAndUsesEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Arrays.asList("set_1", "set_2")))
                .thenReturn(new HashSet<>(Arrays.asList("test_1", "test_2")));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Arrays.asList("set_1", "set_2"));
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_1"), anyMap());
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_2"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsWithNoConfiguredSetsInvalidSetDoesNotQueryStorageForInvalidSet() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "SET!"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(any(), any(), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsWithConfiguredSetsExistingSetQueriesStorageForConfiguredSetOnlyAndUsesEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_2", "set_3"), null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsReturnsCorrectFormat() {
        Set<String> mockNames = new HashSet<>();
        mockNames.add("test_1");
        mockNames.add("test_2");
        when(mSplitsStorage.getNamesByFlagSets(Arrays.asList("set_1", "set_2"))).thenReturn(mockNames);

        Map<String, String> result = mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        assertEquals(2, result.size());
        assertEquals("result_1", result.get("test_1"));
        assertEquals("result_2", result.get("test_2"));
    }

    @Test
    public void getTreatmentsByFlagSetsWithDuplicatedSetDeduplicates() {
        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_1"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
    }

    @Test
    public void getTreatmentsByFlagSetsWithNullSetListReturnsEmpty() {
        Map<String, String> result = mTreatmentManager.getTreatmentsByFlagSets(null, null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
        assertEquals(0, result.size());
    }

    @Test
    public void getTreatmentsByFlagSetsRecordsTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(Collections.singleton("test_1"));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mTelemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_BY_FLAG_SETS), anyLong());
    }

    ///
    @Test
    public void getTreatmentsWithConfigByFlagSetDestroyedDoesNotUseEvaluator() {
        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, true);

        verify(mSplitsStorage).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWithNoConfiguredSetsQueriesStorageAndUsesEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWithNoConfiguredSetsInvalidSetDoesNotQueryStorageNorUseEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("SET!", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWithConfiguredSetsExistingSetQueriesStorageAndUsesEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_2", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetReturnsCorrectFormat() {
        Set<String> mockNames = new HashSet<>();
        mockNames.add("test_1");
        mockNames.add("test_2");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(mockNames);
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));

        Map<String, SplitResult> result = mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, false);

        assertEquals(2, result.size());
        assertEquals("result_1", result.get("test_1").treatment());
        assertEquals("result_2", result.get("test_2").treatment());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetRecordsTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(Collections.singleton("test_1"));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, false);

        verify(mTelemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET), anyLong());
    }

    ///
    @Test
    public void getTreatmentsWithConfigByFlagSetsDestroyedDoesNotUseEvaluator() {
        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Collections.singletonList("set_1"), null, true);

        verify(mSplitsStorage).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithNoConfiguredSetsQueriesStorageAndUsesEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Arrays.asList("set_1", "set_2")))
                .thenReturn(new HashSet<>(Arrays.asList("test_1", "test_2")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Arrays.asList("set_1", "set_2"));
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_1"), anyMap());
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_2"), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithNoConfiguredSetsInvalidSetDoesNotQueryStorageForInvalidSet() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "SET!"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(any(), any(), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithConfiguredSetsExistingSetQueriesStorageForConfiguredSetOnlyAndUsesEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();

        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mFlagSetsFilter = new FlagSetsFilterImpl(Collections.singleton("set_1"));
        initializeTreatmentManager();

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_2", "set_3"), null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsReturnsCorrectFormat() {
        Set<String> mockNames = new HashSet<>();
        mockNames.add("test_1");
        mockNames.add("test_2");
        when(mSplitsStorage.getNamesByFlagSets(Arrays.asList("set_1", "set_2"))).thenReturn(mockNames);

        Map<String, SplitResult> result = mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        assertEquals(2, result.size());
        assertEquals("result_1", result.get("test_1").treatment());
        assertEquals("result_2", result.get("test_2").treatment());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithDuplicatedSetDeduplicates() {
        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_1"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsWithNullSetListReturnsEmpty() {
        Map<String, SplitResult> result = mTreatmentManager.getTreatmentsWithConfigByFlagSets(null, null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
        assertEquals(0, result.size());
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsRecordsTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(Collections.singleton("test_1"));

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mTelemetryStorageProducer).recordLatency(eq(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS), anyLong());
    }

    @Test
    public void getTreatmentsByFlagSetExceptionIsRecordedInTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(any())).thenThrow(new RuntimeException("test"));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mTelemetryStorageProducer).recordException(eq(Method.TREATMENTS_BY_FLAG_SET));
    }

    @Test
    public void getTreatmentsByFlagSetsExceptionIsRecordedInTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(any())).thenThrow(new RuntimeException("test"));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mTelemetryStorageProducer).recordException(eq(Method.TREATMENTS_BY_FLAG_SETS));
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetExceptionIsRecordedInTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(any())).thenThrow(new RuntimeException("test"));

        mTreatmentManager.getTreatmentsWithConfigByFlagSet("set_1", null, false);

        verify(mTelemetryStorageProducer).recordException(eq(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SET));
    }

    @Test
    public void getTreatmentsWithConfigByFlagSetsExceptionIsRecordedInTelemetry() {
        when(mSplitsStorage.getNamesByFlagSets(any())).thenThrow(new RuntimeException("test"));

        mTreatmentManager.getTreatmentsWithConfigByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mTelemetryStorageProducer).recordException(eq(Method.TREATMENTS_WITH_CONFIG_BY_FLAG_SETS));
    }
}
