package io.split.android.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    private Set<String> mConfiguredFlagSets;
    private TreatmentManagerImpl mTreatmentManager;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);

        mConfiguredFlagSets = new HashSet<>();
        when(mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        when(mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)).thenReturn(true);

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
                mConfiguredFlagSets,
                mSplitsStorage);

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
    public void getTreatmentByFlagSetDestroyedDoesNotQueryStorageOrUseEvaluator() {
        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, true);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentByFlagSetWithNoConfiguredSetsQueriesStorageAndUsesEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentByFlagSetWithNoConfiguredSetsInvalidSetDoesNotQueryStorageNorUseEvaluator() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsByFlagSet("SET!", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentByFlagSetWithConfiguredSetsExistingSetQueriesStorageAndUsesEvaluator() {
        mConfiguredFlagSets.add("set_1");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentByFlagSetWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mConfiguredFlagSets.add("set_1");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsByFlagSet("set_2", null, false);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
        verify(mEvaluator, times(0)).getTreatment(any(), any(), any(), anyMap());
    }

    @Test
    public void getTreatmentByFlagSetReturnsCorrectFormat() {
        Set<String> mockNames = new HashSet<>();
        mockNames.add("test_1");
        mockNames.add("test_2");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1"))).thenReturn(mockNames);
        mConfiguredFlagSets.add("set_1");

        Map<String, String> result = mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        assertEquals(2, result.size());
        assertEquals("result_1", result.get("test_1"));
        assertEquals("result_2", result.get("test_2"));
    }

    ///
    @Test
    public void getTreatmentsByFlagSetsDestroyedDoesNotQueryStorageOrUseEvaluator() {
        mTreatmentManager.getTreatmentsByFlagSets(Collections.singletonList("set_1"), null, true);

        verify(mSplitsStorage, times(0)).getNamesByFlagSets(any());
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
        mConfiguredFlagSets.add("set_1");
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_1")));

        mTreatmentManager.getTreatmentsByFlagSets(Arrays.asList("set_1", "set_2"), null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(anyString(), anyString(), eq("test_1"), anyMap());
    }

    @Test
    public void getTreatmentsByFlagSetsWithConfiguredSetsNonExistingSetDoesNotQueryStorageNorUseEvaluator() {
        mConfiguredFlagSets.add("set_1");

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
}
