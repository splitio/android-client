package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventsManagerConfigTest {

    @Test
    public void emptyBuilderCreatesEmptyMaps() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder().build();

        assertTrue(config.getRequireAll().isEmpty());
        assertTrue(config.getRequireAny().isEmpty());
        assertTrue(config.getPrerequisites().isEmpty());
        assertTrue(config.getSuppressedBy().isEmpty());
        assertTrue(config.getExecutionLimits().isEmpty());
        assertTrue(config.getRequireAllMetadataSource().isEmpty());
        assertTrue(config.getRequireAnyMetadataSource().isEmpty());
    }

    @Test
    public void builderCreatesConfigWithAllFields() {
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("E1", "I1", "I2")
                .requireAny("E2", "I3")
                .prerequisite("E1", "E0")
                .suppressedBy("E1", "E2")
                .executionLimit("E1", 3)
                .metadataSource("E1", "I2")
                .metadataSource("E2", Collections.singleton("I3"), "I3")
                .build();

        assertEquals(1, config.getRequireAll().size());
        assertTrue(config.getRequireAll().get("E1").contains("I1"));
        assertTrue(config.getRequireAll().get("E1").contains("I2"));

        // requireAny now stores Set<Set<I>> - single events are wrapped in singleton sets
        assertEquals(1, config.getRequireAny().size());
        Set<Set<String>> requireAnyGroups = config.getRequireAny().get("E2");
        assertEquals(1, requireAnyGroups.size());
        assertTrue(requireAnyGroups.contains(Collections.singleton("I3")));

        assertEquals(1, config.getPrerequisites().size());
        assertTrue(config.getPrerequisites().get("E1").contains("E0"));

        assertEquals(1, config.getSuppressedBy().size());
        assertTrue(config.getSuppressedBy().get("E1").contains("E2"));

        assertEquals(1, config.getExecutionLimits().size());
        assertEquals(Integer.valueOf(3), config.getExecutionLimits().get("E1"));

        assertEquals("I2", config.getRequireAllMetadataSource().get("E1"));
        assertEquals("I3", config.getRequireAnyMetadataSource().get("E2")
                .get(Collections.singleton("I3")));
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
                .metadataSource("E1", "I1")
                .build();

        try {
            config.getRequireAll().put("E2", Collections.singleton("I2"));
            Assert.fail("getRequireAll() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getRequireAny().put("E2", Collections.singleton(Collections.singleton("I2")));
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

        try {
            config.getRequireAllMetadataSource().put("E2", "I2");
            Assert.fail("getRequireAllMetadataSource() should return an unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        try {
            config.getRequireAnyMetadataSource().put("E2", Collections.singletonMap(Collections.singleton("I2"), "I2"));
            Assert.fail("getRequireAnyMetadataSource() should return an unmodifiable map");
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
        assertTrue(config.getRequireAllMetadataSource().isEmpty());
        assertTrue(config.getRequireAnyMetadataSource().isEmpty());

        try {
            config.getRequireAll().put("E1", Collections.singleton("I1"));
            Assert.fail("getRequireAll() from empty() should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void requireAnyWithVarargsCreatesIndividualGroups() {
        // When using requireAny(E, I...), each I should become its own singleton group
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAny("E1", "I1", "I2", "I3")
                .build();

        Set<Set<String>> groups = config.getRequireAny().get("E1");
        assertEquals(3, groups.size());
        assertTrue(groups.contains(Collections.singleton("I1")));
        assertTrue(groups.contains(Collections.singleton("I2")));
        assertTrue(groups.contains(Collections.singleton("I3")));
    }

    @Test
    public void requireAnyWithSetsCreatesAndGroups() {
        // When using requireAny(E, Set<I>...), each Set is an AND group
        Set<String> group1 = new HashSet<>();
        group1.add("I1");
        group1.add("I2");

        Set<String> group2 = new HashSet<>();
        group2.add("I3");
        group2.add("I4");

        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAny("E1", group1, group2)
                .build();

        Set<Set<String>> groups = config.getRequireAny().get("E1");
        assertEquals(2, groups.size());
        assertTrue(groups.contains(group1));
        assertTrue(groups.contains(group2));
    }

    @Test
    public void requireAnyWithMixedGroupSizes() {
        // Groups can have different sizes
        Set<String> singletonGroup = Collections.singleton("I1");

        Set<String> largeGroup = new HashSet<>();
        largeGroup.add("I2");
        largeGroup.add("I3");
        largeGroup.add("I4");
        largeGroup.add("I5");

        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAny("E1", singletonGroup, largeGroup)
                .build();

        Set<Set<String>> groups = config.getRequireAny().get("E1");
        assertEquals(2, groups.size());
        assertTrue(groups.contains(singletonGroup));
        assertTrue(groups.contains(largeGroup));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowOnCircularPrerequisites() {
        EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                .requireAll("B", "I2")
                .prerequisite("A", "B")
                .prerequisite("B", "A")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowOnCircularSuppression() {
        EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                .requireAll("B", "I2")
                .suppressedBy("A", "B")
                .suppressedBy("B", "A")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowOnMixedCircularDependency() {
        // A requires B, B suppressed by A (B -> A from prereq, A -> B from suppression)
        EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                .requireAll("B", "I2")
                .prerequisite("A", "B")
                .suppressedBy("B", "A")
                .build();
    }

    @Test
    public void shouldSortByPrerequisites() {
        // A depends on B, B depends on C
        // Expected order: C, B, A
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                .requireAll("B", "I2")
                .requireAll("C", "I3")
                .prerequisite("A", "B")
                .prerequisite("B", "C")
                .build();

        List<String> order = config.getEvaluationOrder();
        int idxA = order.indexOf("A");
        int idxB = order.indexOf("B");
        int idxC = order.indexOf("C");

        assertTrue("C should come before B", idxC < idxB);
        assertTrue("B should come before A", idxB < idxA);
    }

    @Test
    public void shouldSortBySuppression() {
        // A suppressed by B (B must run first to suppress A)
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                .requireAll("B", "I2")
                .suppressedBy("A", "B")
                .build();

        List<String> order = config.getEvaluationOrder();
        int idxA = order.indexOf("A");
        int idxB = order.indexOf("B");

        assertTrue("B (suppressor) should come before A (suppressed)", idxB < idxA);
    }

    @Test
    public void shouldIncludeEventsFromAllSourcesInSort() {
        // Events might only appear in prerequisites or suppression lists
        // even if they don't have trigger conditions themselves
        EventsManagerConfig<String, String> config = EventsManagerConfig.<String, String>builder()
                .requireAll("A", "I1")
                // B is not explicitly configured with requirements, but is a prerequisite
                .prerequisite("A", "B")
                .build();

        List<String> order = config.getEvaluationOrder();
        assertTrue(order.contains("A"));
        assertTrue(order.contains("B"));
        assertTrue(order.indexOf("B") < order.indexOf("A"));
    }
}
