package io.split.android.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesClient;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.metrics.Metrics;

public class SplitClientImplAttributesTest {

    @Mock
    SplitFactory container;
    @Mock
    AttributesClient attributesClient;
    @Mock
    MySegmentsStorage mySegmentsStorage;
    @Mock
    ImpressionListener impressionListener;
    @Mock
    Metrics metrics;
    @Mock
    SplitsStorage splitsStorage;
    @Mock
    EventPropertiesProcessor eventPropertiesProcessor;
    @Mock
    SyncManager syncManager;
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
                metrics,
                splitClientConfig,
                new SplitEventsManager(splitClientConfig),
                splitsStorage,
                eventPropertiesProcessor,
                syncManager,
                attributesClient
        );
    }

    @Test
    public void setAttributeCallsSetAttributeOnAttributeClient() {
        AttributesClient attributesClient = mock(AttributesClient.class);

        splitClient.setAttribute("key", "value");

        verify(attributesClient).setAttribute("key", "value");
    }
}
