package io.split.android.client;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
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

    private Set<String> mConfiguredFlagSets = new HashSet<>();
    private TreatmentManagerImpl mTreatmentManager;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);

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

        when(mEvaluator.getTreatment(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(new EvaluationResult("test", "label"));
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
    public void getTreatmentByFlagSetWithNoConfiguredSets() {
        when(mSplitsStorage.getNamesByFlagSets(Collections.singletonList("set_1")))
                .thenReturn(new HashSet<>(Collections.singletonList("test_split")));

        mTreatmentManager.getTreatmentsByFlagSet("set_1", null, false);

        verify(mSplitsStorage).getNamesByFlagSets(Collections.singletonList("set_1"));
        verify(mEvaluator).getTreatment(eq("matching_key"), eq("bucketing_key"), eq("test_split"), anyMap());
    }
}
