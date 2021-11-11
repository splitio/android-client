package io.split.android.client;

import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

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
    public void setAttributeCallsSetAttributeOnAttributesClient() {

        splitClient.setAttribute("key", "value");

        verify(attributesClient).setAttribute("key", "value");
    }

    @Test
    public void setAttributeReturnsSetAttributeValueFromAttributesClient() {
        Mockito.when(attributesClient.setAttribute("key", "value")).thenReturn(true);

        boolean result = splitClient.setAttribute("key", "value");

        verify(attributesClient).setAttribute("key", "value");
        Assert.assertTrue(result);
    }

    @Test
    public void getAttributeReturnsAttributeFromAttributesClient() {
        Mockito.when(attributesClient.getAttribute("key")).thenReturn("value");

        Object value = splitClient.getAttribute("key");

        verify(attributesClient).getAttribute("key");
        Assert.assertEquals("value", value);
    }

    @Test
    public void setAttributesCallsSetAttributesOnAttributesClient() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        splitClient.setAttributes(testValues);

        verify(attributesClient).setAttributes(testValues);
    }

    @Test
    public void setAttributesReturnsSetAttributesValueFromAttributesClient() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        Mockito.when(attributesClient.setAttributes(testValues)).thenReturn(true);

        boolean result = splitClient.setAttributes(testValues);

        Assert.assertTrue(result);
    }

    @Test
    public void getAllAttributesReturnsValuesFromAttributesClient() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        Mockito.when(attributesClient.getAllAttributes()).thenReturn(testValues);

        Map<String, Object> allAttributes = splitClient.getAllAttributes();

        verify(attributesClient).getAllAttributes();
        Assert.assertEquals(testValues, allAttributes);
    }

    @Test
    public void removeAttributeCallsRemoveOnAttributesClient() {

        splitClient.removeAttribute("key");

        verify(attributesClient).removeAttribute("key");
    }

    @Test
    public void clearAttributesCallsClearAttributesOnAttributesClient() {

        splitClient.clearAttributes();

        verify(attributesClient).clearAttributes();
    }
}
