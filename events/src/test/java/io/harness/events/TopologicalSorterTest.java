package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopologicalSorterTest {

    @Test
    public void emptySetReturnsEmptyList() {
        TopologicalSorter<String> sorter = new TopologicalSorter<>(
                Collections.emptySet(),
                Collections.emptyMap()
        );

        List<String> result = sorter.sort();
        assertTrue(result.isEmpty());
    }

    @Test
    public void singleNodeReturnsSingletonList() {
        Set<String> nodes = Collections.singleton("A");
        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
    }

    @Test
    public void independentNodesCanBeInAnyOrder() {
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.emptySet());
        dependencies.put("B", Collections.emptySet());
        dependencies.put("C", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(3, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.contains("C"));
    }

    @Test
    public void simpleChainRespectsOrder() {
        // A depends on B, B depends on C
        // Expected: C, B, A
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.singleton("B"));
        dependencies.put("B", Collections.singleton("C"));
        dependencies.put("C", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(3, result.size());
        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("C should come before B", idxC < idxB);
        assertTrue("B should come before A", idxB < idxA);
    }

    @Test
    public void multipleDependenciesRespected() {
        // A depends on B and C
        // Expected: B and C before A (order between B and C doesn't matter)
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");

        Map<String, Set<String>> dependencies = new HashMap<>();
        Set<String> aDeps = new HashSet<>();
        aDeps.add("B");
        aDeps.add("C");
        dependencies.put("A", aDeps);
        dependencies.put("B", Collections.emptySet());
        dependencies.put("C", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(3, result.size());
        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("B should come before A", idxB < idxA);
        assertTrue("C should come before A", idxC < idxA);
    }

    @Test
    public void diamondDependencyResolved() {
        //     B
        //   /   \
        // A       D
        //   \   /
        //     C
        // A depends on B and C, B depends on D, C depends on D
        // Expected: D before B and C, B and C before A
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");
        nodes.add("D");

        Map<String, Set<String>> dependencies = new HashMap<>();
        Set<String> aDeps = new HashSet<>();
        aDeps.add("B");
        aDeps.add("C");
        dependencies.put("A", aDeps);
        dependencies.put("B", Collections.singleton("D"));
        dependencies.put("C", Collections.singleton("D"));
        dependencies.put("D", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(4, result.size());
        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");
        int idxD = result.indexOf("D");

        assertTrue("D should come before B", idxD < idxB);
        assertTrue("D should come before C", idxD < idxC);
        assertTrue("B should come before A", idxB < idxA);
        assertTrue("C should come before A", idxC < idxA);
    }

    @Test(expected = IllegalStateException.class)
    public void detectsDirectCycle() {
        // A depends on B, B depends on A
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.singleton("B"));
        dependencies.put("B", Collections.singleton("A"));

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        sorter.sort(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsSelfCycle() {
        // A depends on itself
        Set<String> nodes = Collections.singleton("A");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.singleton("A"));

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        sorter.sort(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsLongCycle() {
        // A -> B -> C -> A
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.singleton("B"));
        dependencies.put("B", Collections.singleton("C"));
        dependencies.put("C", Collections.singleton("A"));

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        sorter.sort(); // Should throw
    }

    @Test
    public void handlesMissingDependencyEntries() {
        // If a node is not in dependencies map, it should be treated as having no dependencies
        Set<String> nodes = new HashSet<>();
        nodes.add("A");
        nodes.add("B");

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.singleton("B"));
        // B is not in dependencies map

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        assertEquals(2, result.size());
        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        assertTrue("B should come before A", idxB < idxA);
    }

    @Test
    public void resultIsUnmodifiable() {
        Set<String> nodes = Collections.singleton("A");
        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("A", Collections.emptySet());

        TopologicalSorter<String> sorter = new TopologicalSorter<>(nodes, dependencies);
        List<String> result = sorter.sort();

        try {
            result.add("B");
            fail("Result should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}

