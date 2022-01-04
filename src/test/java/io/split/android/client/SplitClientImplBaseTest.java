package io.split.android.client;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryEvaluationProducer;
import io.split.android.client.validators.EventValidator;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.engine.experiments.SplitParser;

public abstract class SplitClientImplBaseTest {

    @Mock
    protected SplitFactory container;
    @Mock
    protected AttributesManager attributesManager;
    @Mock
    protected MySegmentsStorage mySegmentsStorage;
    @Mock
    protected ImpressionListener impressionListener;
    @Mock
    protected SplitsStorage splitsStorage;
    @Mock
    protected EventPropertiesProcessor eventPropertiesProcessor;
    @Mock
    protected SyncManager syncManager;
    @Mock
    protected TelemetryEvaluationProducer telemetryEvaluationProducer;
    @Mock
    protected EventValidator eventValidator;
    @Mock
    protected TreatmentManager treatmentManager;
    @Mock
    protected SplitValidator splitValidator;

    protected SplitClientImpl splitClient;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder().build();

        splitClient = new SplitClientImpl(
                container,
                new Key("test_key"),
                new SplitParser(mySegmentsStorage),
                impressionListener,
                splitClientConfig,
                new SplitEventsManager(splitClientConfig),
                splitsStorage,
                eventPropertiesProcessor,
                eventValidator,
                syncManager,
                attributesManager,
                telemetryEvaluationProducer,
                treatmentManager,
                splitValidator
        );
    }
}
