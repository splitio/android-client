package io.split.android.client.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class MetadataKeyTest {

    @Test
    public void nameReturnsConstructorParameter() {
        MetadataKey<String> key = new MetadataKey<>("testKey");

        assertEquals("testKey", key.name());
    }

    @Test
    public void nameReturnsEmptyStringWhenConstructedWithEmptyString() {
        MetadataKey<String> key = new MetadataKey<>("");

        assertEquals("", key.name());
    }

    @Test
    public void keyCanBeCreatedWithDifferentTypes() {
        MetadataKey<String> stringKey = new MetadataKey<>("stringKey");
        MetadataKey<Integer> intKey = new MetadataKey<>("intKey");
        MetadataKey<Boolean> boolKey = new MetadataKey<>("boolKey");

        assertNotNull(stringKey.name());
        assertNotNull(intKey.name());
        assertNotNull(boolKey.name());
    }
}

