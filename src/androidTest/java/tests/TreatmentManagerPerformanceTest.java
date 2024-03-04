package tests;

import android.os.SystemClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.Evaluator;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.validators.KeyValidator;
import io.split.android.client.validators.SplitFilterValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;

public class TreatmentManagerPerformanceTest {

    @Mock
    private Evaluator evaluator;
    @Mock
    private KeyValidator keyValidator;
    @Mock
    private SplitValidator splitValidator;
    @Mock
    private ImpressionListener impressionListener;
    @Mock
    private ListenableEventsManager eventsManager;
    @Mock
    private AttributesManager attributesManager;
    @Mock
    private AttributesMerger attributesMerger;
    @Mock
    private TelemetryStorageProducer telemetryStorageProducer;
    @Mock
    private SplitsStorage mSplitsStorage;
    @Mock
    private SplitFilterValidator mFlagSetsValidator;

    private FlagSetsFilter mFlagSetsFilter;
    private TreatmentManagerImpl treatmentManager;
    private TreatmentManagerImpl mTreatmentManager;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setUp() {
        mTreatmentManager = new TreatmentManagerImpl(
                "test_key",
                "test_key",
                evaluator,
                keyValidator,
                splitValidator,
                impressionListener,
                SplitClientConfig.builder().build().labelsEnabled(),
                eventsManager,
                attributesManager,
                attributesMerger,
                telemetryStorageProducer,
                mFlagSetsFilter,
                mSplitsStorage,
                new ValidationMessageLoggerImpl(),
                mFlagSetsValidator);
    }

    @After
    public void tearDown() {
        try {
            mAutoCloseable.close();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void getTreatment() {
        long start = SystemClock.elapsedRealtime();
        for (int i = 0; i < 1000; i++) {
            mTreatmentManager.getTreatment("test_feature", null, false);
        }
        long end = SystemClock.elapsedRealtime();
        long elapsedTime = end - start;
        System.out.println("getTreatment: " + elapsedTime + "ms");
    }
}
