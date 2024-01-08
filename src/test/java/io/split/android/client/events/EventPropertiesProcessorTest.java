package io.split.android.client.events;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.EventPropertiesProcessor;
import io.split.android.client.EventPropertiesProcessorImpl;
import io.split.android.client.ProcessedEventProperties;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Utils;
import io.split.android.client.validators.ValidationConfig;

public class EventPropertiesProcessorTest {

    private EventPropertiesProcessor processor = new EventPropertiesProcessorImpl();
    private final static long MAX_BYTES = ValidationConfig.getInstance().getMaximumEventPropertyBytes();
    private final static int MAX_COUNT = 300;

    @Before
    public void setup() {
    }

    @Test
    public void sizeInBytesValidation() {
        Map<String, Object> properties = new HashMap<>();
        int maxCount = (int) (MAX_BYTES / 1024);
        int count = 1;
        while (count <= maxCount) {
            properties.put("key" + count, Utils.repeat("a", 1021)); // 1025 bytes
            count++;
        }
        ProcessedEventProperties result = processor.process(properties);

        Assert.assertFalse(result.isValid());
    }

    @Test
    public void invalidPropertyType() {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            properties.put("key" + i, "the value");
        }
        for (int i = 0; i < 10; i++) {
            properties.put("key" + i, new Split());
        }
        ProcessedEventProperties result = processor.process(properties);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(10, result.getProperties().size());
    }

    @Test
    public void nullValues() {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            properties.put("key" + i, "the value");
        }
        for (int i = 10; i < 20; i++) {
            properties.put("key" + i + 10, null);
        }
        ProcessedEventProperties result = processor.process(properties);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(20, result.getProperties().size());
    }

    @Test
    public void totalBytes() {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            properties.put("k" + i, "10 bytes");
        }
        ProcessedEventProperties result = processor.process(properties);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(100, result.getSizeInBytes());
    }
}
