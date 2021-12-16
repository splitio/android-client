package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryEvaluationProducer;
import io.split.android.client.validators.EventValidator;
import io.split.android.engine.experiments.SplitParser;

public class SplitClientImplTelemetryTest {

    @Mock
    SplitFactory container;
    @Mock
    AttributesManager attributesManager;
    @Mock
    MySegmentsStorage mySegmentsStorage;
    @Mock
    ImpressionListener impressionListener;
    @Mock
    SplitsStorage splitsStorage;
    @Mock
    EventPropertiesProcessor eventPropertiesProcessor;
    @Mock
    SyncManager syncManager;
    @Mock
    TelemetryEvaluationProducer telemetryEvaluationProducer;
    @Mock
    EventValidator eventValidator;

    private SplitClientImpl splitClient;

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
                telemetryEvaluationProducer
        );
    }

    @Test
    public void trackRecordsLatencyInEvaluationProducer() {
        ProcessedEventProperties processedEventProperties = mock(ProcessedEventProperties.class);
        when(processedEventProperties.isValid()).thenReturn(true);
        when(eventPropertiesProcessor.process(null)).thenReturn(processedEventProperties);
        when(eventValidator.validate(any(), eq(false))).thenReturn(null);

        splitClient.track("any");

        verify(telemetryEvaluationProducer).recordLatency(eq(Method.TRACK), anyLong());
    }
}
