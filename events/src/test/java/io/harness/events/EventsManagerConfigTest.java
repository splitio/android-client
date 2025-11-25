package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class EventsManagerConfigTest {

    @Test
    public void emptyBuilderCreatesEmptyMaps() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder().build();

        assertTrue(config.getRequireAll().isEmpty());
        assertTrue(config.getRequireAny().isEmpty());
        assertTrue(config.getPrerequisites().isEmpty());
        assertTrue(config.getSuppressedBy().isEmpty());
        assertTrue(config.getExecutionLimits().isEmpty());
    }

    @Test
    public void builderCreatesConfigWithAllFields() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("E1", "I1", "I2")
                .requireAny("E2", "I3")
                .prerequisite("E1", "E0")
                .suppressedBy("E1", "E2")
                .executionLimit("E1", 3)
                .build();

        assertEquals(1, config.getRequireAll().size());
        assertTrue(config.getRequireAll().get("E1").contains("I1"));
        assertTrue(config.getRequireAll().get("E1").contains("I2"));

        assertEquals(1, config.getRequireAny().size());
        assertTrue(config.getRequireAny().get("E2").contains("I3"));

        assertEquals(1, config.getPrerequisites().size());
        assertTrue(config.getPrerequisites().get("E1").contains("E0"));

        assertEquals(1, config.getSuppressedBy().size());
        assertTrue(config.getSuppressedBy().get("E1").contains("E2"));

        assertEquals(1, config.getExecutionLimits().size());
        assertEquals(Integer.valueOf(3), config.getExecutionLimits().get("E1"));
    }

    @Test
    public void builderAllowsMultiplePrerequisites() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .prerequisite("E1", "E0")
                .prerequisite("E1", "E2")
                .build();

        assertEquals(1, config.getPrerequisites().size());
        assertEquals(2, config.getPrerequisites().get("E1").size());
        assertTrue(config.getPrerequisites().get("E1").contains("E0"));
        assertTrue(config.getPrerequisites().get("E1").contains("E2"));
    }

    @Test
    public void builderAllowsMultipleSuppressors() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .suppressedBy("E1", "E2")
                .suppressedBy("E1", "E3")
                .build();

        assertEquals(1, config.getSuppressedBy().size());
        assertEquals(2, config.getSuppressedBy().get("E1").size());
        assertTrue(config.getSuppressedBy().get("E1").contains("E2"));
        assertTrue(config.getSuppressedBy().get("E1").contains("E3"));
    }

    @Test
    public void returnedMapsAreUnmodifiable() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("E1", "I1")
                .requireAny("E1", "I1")
                .prerequisite("E1", "E0")
                .suppressedBy("E1", "E2")
                .executionLimit("E1", 3)
                .build();

        try {
            config.getRequireAll().put("E2", Collections.singleton("I2"));
            Assert.fail("getRequireAll() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getRequireAny().put("E2", Collections.singleton("I2"));
            Assert.fail("getRequireAny() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getPrerequisites().put("E2", Collections.singleton("E3"));
            Assert.fail("getPrerequisites() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getSuppressedBy().put("E2", Collections.singleton("E3"));
            Assert.fail("getSuppressedBy() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getExecutionLimits().put("E2", 5);
            Assert.fail("getExecutionLimits() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void emptyMethodReturnsEmptyUnmodifiableConfig() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>empty();

        assertTrue(config.getRequireAll().isEmpty());
        assertTrue(config.getRequireAny().isEmpty());
        assertTrue(config.getPrerequisites().isEmpty());
        assertTrue(config.getSuppressedBy().isEmpty());
        assertTrue(config.getExecutionLimits().isEmpty());

        try {
            config.getRequireAll().put("E1", Collections.singleton("I1"));
            Assert.fail("getRequireAll() from empty() should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
