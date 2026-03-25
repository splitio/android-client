package io.split.android.client.validators;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.tracker.TrackerLogger;
import io.split.android.client.tracker.TrackerPropertyValidator;

public class PropertyValidatorTest {

    private final TrackerPropertyValidator processor = new PropertyValidatorImpl(mock(TrackerLogger.class));
    private final static long MAX_BYTES = ValidationConfig.getInstance().getMaximumEventPropertyBytes();

    @Before
    public void setup() {
    }

    @Test
    public void sizeInBytesValidation() {
        Map<String, Object> properties = new HashMap<>();
        int maxCount = (int) (MAX_BYTES / 1024);
        int count = 1;
        while (count <= maxCount) {
            properties.put("key" + count, repeat("a", 1021)); // 1025 bytes
            count++;
        }
        TrackerPropertyValidator.TrackerPropertyResult result = validate(properties);

        Assert.assertFalse(result.isValid());
    }

    private String repeat(String str, int count) {
        StringBuilder builder = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }

    @Test
    public void invalidPropertyType() {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            properties.put("key" + i, "the value");
        }
        // Add invalid property types (objects that are not Number, Boolean, or String)
        for (int i = 0; i < 10; i++) {
            properties.put("key" + i, new Object());
        }
        TrackerPropertyValidator.TrackerPropertyResult result = validate(properties);

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
        TrackerPropertyValidator.TrackerPropertyResult result = validate(properties);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(20, result.getProperties().size());
    }

    @Test
    public void totalBytes() {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            properties.put("k" + i, "10 bytes");
        }
        TrackerPropertyValidator.TrackerPropertyResult result = validate(properties);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals(100, result.getSizeInBytes());
    }

    private TrackerPropertyValidator.TrackerPropertyResult validate(Map<String, Object> properties) {
        return processor.validate(properties, 0, "test");
    }
}
