package io.harness.events;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventsManagerConfigTest {

    @Test
    public void nullInputMapsCreateEmptyMaps() {
        EventsManagerConfig<String, String> config =
                new EventsManagerConfig<>(null, null, null, null, null);

        Assert.assertTrue(config.getRequireAll().isEmpty());
        Assert.assertTrue(config.getRequireAny().isEmpty());
        Assert.assertTrue(config.getPrerequisites().isEmpty());
        Assert.assertTrue(config.getSuppressedBy().isEmpty());
        Assert.assertTrue(config.getExecutionLimits().isEmpty());
    }

    @Test
    public void mutationsToInputMapsDoNotModifyConfig() {
        Map<String, Set<String>> requireAll = new HashMap<>();
        Map<String, Set<String>> requireAny = new HashMap<>();
        Map<String, Set<String>> prerequisites = new HashMap<>();
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        Map<String, Integer> executionLimits = new HashMap<>();

        Set<String> internals = new HashSet<>();
        internals.add("I1");

        requireAll.put("E1", internals);
        requireAny.put("E1", internals);
        prerequisites.put("E1", Collections.singleton("E0"));
        suppressedBy.put("E1", Collections.singleton("E2"));
        executionLimits.put("E1", 3);

        EventsManagerConfig<String, String> config =
                new EventsManagerConfig<>(requireAll, requireAny, prerequisites, suppressedBy, executionLimits);

        // Mutate the original maps after construction
        requireAll.put("E2", Collections.singleton("I2"));
        requireAny.clear();
        prerequisites.put("E2", Collections.singleton("E3"));
        suppressedBy.remove("E1");
        executionLimits.put("E2", 5);

        Map<String, Set<String>> requireAllFromConfig = config.getRequireAll();
        Map<String, Set<String>> requireAnyFromConfig = config.getRequireAny();
        Map<String, Set<String>> prerequisitesFromConfig = config.getPrerequisites();
        Map<String, Set<String>> suppressedByFromConfig = config.getSuppressedBy();
        Map<String, Integer> executionLimitsFromConfig = config.getExecutionLimits();

        Assert.assertEquals(1, requireAllFromConfig.size());
        Assert.assertTrue(requireAllFromConfig.containsKey("E1"));
        Assert.assertFalse(requireAllFromConfig.containsKey("E2"));

        Assert.assertEquals(1, requireAnyFromConfig.size());
        Assert.assertTrue(requireAnyFromConfig.containsKey("E1"));

        Assert.assertEquals(1, prerequisitesFromConfig.size());
        Assert.assertTrue(prerequisitesFromConfig.containsKey("E1"));
        Assert.assertFalse(prerequisitesFromConfig.containsKey("E2"));

        Assert.assertEquals(1, suppressedByFromConfig.size());
        Assert.assertTrue(suppressedByFromConfig.containsKey("E1"));

        Assert.assertEquals(1, executionLimitsFromConfig.size());
        Assert.assertTrue(executionLimitsFromConfig.containsKey("E1"));
        Assert.assertFalse(executionLimitsFromConfig.containsKey("E2"));
    }

    @Test
    public void returnedMapsAreUnmodifiable() {
        Map<String, Set<String>> requireAll = new HashMap<>();
        requireAll.put("E1", Collections.singleton("I1"));

        Map<String, Set<String>> requireAny = new HashMap<>();
        requireAny.put("E1", Collections.singleton("I1"));

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("E1", Collections.singleton("E0"));

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("E1", Collections.singleton("E2"));

        Map<String, Integer> executionLimits = new HashMap<>();
        executionLimits.put("E1", 3);

        EventsManagerConfig<String, String> config =
                new EventsManagerConfig<>(requireAll, requireAny, prerequisites, suppressedBy, executionLimits);

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

        Assert.assertTrue(config.getRequireAll().isEmpty());
        Assert.assertTrue(config.getRequireAny().isEmpty());
        Assert.assertTrue(config.getPrerequisites().isEmpty());
        Assert.assertTrue(config.getSuppressedBy().isEmpty());
        Assert.assertTrue(config.getExecutionLimits().isEmpty());

        try {
            config.getRequireAll().put("E1", Collections.singleton("I1"));
            Assert.fail("getRequireAll() from empty() should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
