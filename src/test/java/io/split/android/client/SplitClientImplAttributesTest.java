package io.split.android.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributeClient;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.SplitClientImplFactory;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.metrics.Metrics;

public class SplitClientImplAttributesTest {

    @Mock
    SplitFactory container;
    @Mock
    AttributeClient attributeClient;
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
                attributeClient
        );
    }

    @Test
    public void setAttributeCallsSetAttributeOnAttributeClient() {
        AttributeClient attributeClient = mock(AttributeClient.class);

        splitClient.setAttribute("key", "value");

        verify(attributeClient).setAttribute("key", "value");
    }
}
