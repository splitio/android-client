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
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;

public class SplitClientImplAttributesTest {

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
                syncManager,
                attributesManager
        );
    }

    @Test
    public void setAttributeCallsSetAttributeOnAttributesManager() {

        splitClient.setAttribute("key", "value");

        verify(attributesManager).setAttribute("key", "value");
    }

    @Test
    public void setAttributeReturnsSetAttributeValueFromAttributesManager() {
        Mockito.when(attributesManager.setAttribute("key", "value")).thenReturn(true);

        boolean result = splitClient.setAttribute("key", "value");

        verify(attributesManager).setAttribute("key", "value");
        Assert.assertTrue(result);
    }

    @Test
    public void getAttributeReturnsAttributeFromAttributesManager() {
        Mockito.when(attributesManager.getAttribute("key")).thenReturn("value");

        Object value = splitClient.getAttribute("key");

        verify(attributesManager).getAttribute("key");
        Assert.assertEquals("value", value);
    }

    @Test
    public void setAttributesCallsSetAttributesOnAttributesManager() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        splitClient.setAttributes(testValues);

        verify(attributesManager).setAttributes(testValues);
    }

    @Test
    public void setAttributesReturnsSetAttributesValueFromAttributesManager() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        Mockito.when(attributesManager.setAttributes(testValues)).thenReturn(true);

        boolean result = splitClient.setAttributes(testValues);

        Assert.assertTrue(result);
    }

    @Test
    public void getAllAttributesReturnsValuesFromAttributesManager() {
        Map<String, Object> testValues = new HashMap<>();
        testValues.put("key1", "value1");
        testValues.put("key2", 200.05);

        Mockito.when(attributesManager.getAllAttributes()).thenReturn(testValues);

        Map<String, Object> allAttributes = splitClient.getAllAttributes();

        verify(attributesManager).getAllAttributes();
        Assert.assertEquals(testValues, allAttributes);
    }

    @Test
    public void removeAttributeCallsRemoveOnAttributesManager() {

        splitClient.removeAttribute("key");

        verify(attributesManager).removeAttribute("key");
    }

    @Test
    public void clearAttributesCallsClearAttributesOnAttributesManager() {

        splitClient.clearAttributes();

        verify(attributesManager).clearAttributes();
    }
}
