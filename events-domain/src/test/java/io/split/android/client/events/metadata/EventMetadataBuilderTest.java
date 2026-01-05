package io.split.android.client.events.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.api.MetadataKey;
import io.split.android.client.api.SdkUpdateMetadataKeys;

public class EventMetadataBuilderTest {

    @Mock
    private MetadataValidator mValidator;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void putStringUsesValidator() {
        when(mValidator.isValidValue(any())).thenReturn(true);

        new EventMetadataBuilder(mValidator)
                .put("key", "value");

        verify(mValidator).isValidValue("value");
    }

    @Test
    public void putNumberUsesValidator() {
        when(mValidator.isValidValue(any())).thenReturn(true);

        new EventMetadataBuilder(mValidator)
                .put("key", 42);

        verify(mValidator).isValidValue(42);
    }

    @Test
    public void putBooleanUsesValidator() {
        when(mValidator.isValidValue(any())).thenReturn(true);

        new EventMetadataBuilder(mValidator)
                .put("key", true);

        verify(mValidator).isValidValue(true);
    }

    @Test
    public void putListUsesValidator() {
        when(mValidator.isValidValue(any())).thenReturn(true);
        List<String> list = Arrays.asList("a", "b");

        new EventMetadataBuilder(mValidator)
                .put("key", list);

        verify(mValidator).isValidValue(list);
    }

    @Test
    public void putIgnoresValueWhenValidatorReturnsFalse() {
        when(mValidator.isValidValue(any())).thenReturn(false);

        EventMetadata metadata = new EventMetadataBuilder(mValidator)
                .put("key", "value")
                .build();

        MetadataKey<String> KEY = new MetadataKey<>("key");
        assertFalse(metadata.containsKey(KEY));
    }

    @Test
    public void putIncludesValueWhenValidatorReturnsTrue() {
        when(mValidator.isValidValue(any())).thenReturn(true);

        EventMetadata metadata = new EventMetadataBuilder(mValidator)
                .put("key", "value")
                .build();

        MetadataKey<String> KEY = new MetadataKey<>("key");
        assertEquals("value", metadata.get(KEY));
    }

    @Test
    public void buildCreatesEmptyMetadataWhenNothingAdded() {
        EventMetadata metadata = new EventMetadataBuilder().build();

        assertTrue(metadata.isEmpty());
    }

    @Test
    public void putStringAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("key", "value")
                .build();

        MetadataKey<String> KEY = new MetadataKey<>("key");
        assertEquals("value", metadata.get(KEY));
    }

    @Test
    public void putIntegerAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("count", 42)
                .build();

        MetadataKey<Integer> COUNT = new MetadataKey<>("count");
        assertEquals(Integer.valueOf(42), metadata.get(COUNT));
    }

    @Test
    public void putLongAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("timestamp", 1234567890L)
                .build();

        MetadataKey<Long> TIMESTAMP = new MetadataKey<>("timestamp");
        assertEquals(Long.valueOf(1234567890L), metadata.get(TIMESTAMP));
    }

    @Test
    public void putDoubleAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("rate", 3.14)
                .build();

        MetadataKey<Double> RATE = new MetadataKey<>("rate");
        assertEquals(Double.valueOf(3.14), metadata.get(RATE));
    }

    @Test
    public void putBooleanTrueAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("enabled", true)
                .build();

        MetadataKey<Boolean> ENABLED = new MetadataKey<>("enabled");
        assertEquals(Boolean.TRUE, metadata.get(ENABLED));
    }

    @Test
    public void putBooleanFalseAddsValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("disabled", false)
                .build();

        MetadataKey<Boolean> DISABLED = new MetadataKey<>("disabled");
        assertEquals(Boolean.FALSE, metadata.get(DISABLED));
    }

    @Test
    public void putListOfStringsAddsValue() {
        List<String> flags = Arrays.asList("flag_1", "flag_2", "flag_3");

        EventMetadata metadata = new EventMetadataBuilder()
                .put("updatedFlags", flags)
                .build();

        assertEquals(flags, metadata.get(SdkUpdateMetadataKeys.UPDATED_FLAGS));
    }

    @Test
    public void chainingMultiplePutsWorks() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("string", "text")
                .put("number", 100)
                .put("flag", true)
                .put("list", Arrays.asList("a", "b"))
                .build();

        assertEquals(4, metadata.size());
        assertEquals("text", metadata.get(new MetadataKey<String>("string")));
        assertEquals(Integer.valueOf(100), metadata.get(new MetadataKey<Integer>("number")));
        assertEquals(Boolean.TRUE, metadata.get(new MetadataKey<Boolean>("flag")));
        assertEquals(Arrays.asList("a", "b"), metadata.get(new MetadataKey<List<String>>("list")));
    }

    @Test
    public void overwritingKeyUsesLastValue() {
        EventMetadata metadata = new EventMetadataBuilder()
                .put("key", "first")
                .put("key", "second")
                .build();

        MetadataKey<String> KEY = new MetadataKey<>("key");
        assertEquals("second", metadata.get(KEY));
    }

    @Test
    public void buildReturnsNewInstanceEachTime() {
        EventMetadataBuilder builder = new EventMetadataBuilder()
                .put("key", "value");

        EventMetadata metadata1 = builder.build();
        EventMetadata metadata2 = builder.build();

        MetadataKey<String> KEY = new MetadataKey<>("key");
        assertEquals(metadata1.get(KEY), metadata2.get(KEY));
    }
}
