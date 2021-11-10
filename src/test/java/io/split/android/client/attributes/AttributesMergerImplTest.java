package io.split.android.client.attributes;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AttributesMergerImplTest {

    private final AttributesMergerImpl attributesMerger = new AttributesMergerImpl();

    @Test
    public void oneTimeAttributesTakePrecedenceOverStoredAttributes() {
        Map<String, Object> storedAttributes = new HashMap<>();
        storedAttributes.put("key1", "value1");
        storedAttributes.put("key2", new ArrayList<>());
        storedAttributes.put("key3", 120);

        Map<String, Object> oneTimeAttributes = new HashMap<>();
        oneTimeAttributes.put("key1", "newValue1");
        oneTimeAttributes.put("key5", false);

        Map<String, Object> expectedAttributes = new HashMap<>();
        expectedAttributes.put("key1", "newValue1");
        expectedAttributes.put("key2", new ArrayList<>());
        expectedAttributes.put("key3", 120);
        expectedAttributes.put("key5", false);

        Map<String, Object> mergedAttributes = attributesMerger.merge(storedAttributes, oneTimeAttributes);

        Assert.assertEquals(expectedAttributes, mergedAttributes);
    }
}