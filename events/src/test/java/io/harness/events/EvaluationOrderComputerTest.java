package io.harness.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvaluationOrderComputerTest {

    @Test
    public void emptyInputsReturnEmptyList() {
        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        List<String> result = computer.compute();
        assertTrue(result.isEmpty());
    }

    @Test
    public void nullInputsReturnEmptyList() {
        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(null, null, null);

        List<String> result = computer.compute();
        assertTrue(result.isEmpty());
    }

    @Test
    public void includesAllEventsFromInput() {
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        assertEquals(3, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.contains("C"));
    }

    @Test
    public void includesEventsFromPrerequisiteValues() {
        Set<String> allEvents = Collections.singleton("A");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B")); // B is only in values

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
    }

    @Test
    public void includesEventsFromSuppressorValues() {
        Set<String> allEvents = Collections.singleton("A");

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B")); // B is only in values

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                Collections.emptyMap(),
                suppressedBy
        );
        List<String> result = computer.compute();

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
    }

    @Test
    public void buildsDependencyGraphFromPrerequisites() {
        // A depends on B, B depends on C
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));
        prerequisites.put("B", Collections.singleton("C"));
        prerequisites.put("C", Collections.emptySet());

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("C should come before B", idxC < idxB);
        assertTrue("B should come before A", idxB < idxA);
    }

    @Test
    public void buildsDependencyGraphFromSuppression() {
        // A suppressed by B (B must come before A)
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                Collections.emptyMap(),
                suppressedBy
        );
        List<String> result = computer.compute();

        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");

        assertTrue("B (suppressor) should come before A (suppressed)", idxB < idxA);
    }

    @Test
    public void combinesPrerequisitesAndSuppression() {
        // A depends on B (prerequisite), C suppressed by B
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("C", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                suppressedBy
        );
        List<String> result = computer.compute();

        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("B should come before A", idxB < idxA);
        assertTrue("B should come before C", idxB < idxC);
    }

    @Test
    public void handlesMultiplePrerequisites() {
        // A depends on both B and C
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        Set<String> aDeps = new HashSet<>();
        aDeps.add("B");
        aDeps.add("C");
        prerequisites.put("A", aDeps);

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("B should come before A", idxB < idxA);
        assertTrue("C should come before A", idxC < idxA);
    }

    @Test
    public void handlesMultipleSuppressors() {
        // A suppressed by both B and C
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        Set<String> aSuppressors = new HashSet<>();
        aSuppressors.add("B");
        aSuppressors.add("C");
        suppressedBy.put("A", aSuppressors);

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                Collections.emptyMap(),
                suppressedBy
        );
        List<String> result = computer.compute();

        int idxA = result.indexOf("A");
        int idxB = result.indexOf("B");
        int idxC = result.indexOf("C");

        assertTrue("B should come before A", idxB < idxA);
        assertTrue("C should come before A", idxC < idxA);
    }

    @Test(expected = IllegalStateException.class)
    public void detectsCircularDependencyThroughPrerequisites() {
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));
        prerequisites.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                Collections.emptyMap()
        );
        computer.compute(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsCircularDependencyThroughSuppression() {
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));
        suppressedBy.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                Collections.emptyMap(),
                suppressedBy
        );
        computer.compute(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsCircularDependencyThroughMixedRelationships() {
        // A depends on B (prerequisite), B suppressed by A
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");

        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                suppressedBy
        );
        computer.compute(); // Should throw
    }

    @Test
    public void eventsWithNoDependenciesAreIncluded() {
        // Events without prerequisites or suppression should still be in the result
        Set<String> allEvents = new HashSet<>();
        allEvents.add("A");
        allEvents.add("B");
        allEvents.add("C");

        // Only A has a dependency, B and C are independent
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                allEvents,
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        assertEquals(3, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.contains("C"));

        // B should come before A (dependency), C can be anywhere
        assertTrue("B should come before A", result.indexOf("B") < result.indexOf("A"));
    }
}
