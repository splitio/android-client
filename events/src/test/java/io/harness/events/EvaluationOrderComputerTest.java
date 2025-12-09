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
    public void emptyMapsReturnEmptyList() {
        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        List<String> result = computer.compute();
        assertTrue(result.isEmpty());
    }

    @Test
    public void nullMapsReturnEmptyList() {
        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(null, null);

        List<String> result = computer.compute();
        assertTrue(result.isEmpty());
    }

    @Test
    public void gathersEventsFromPrerequisiteKeys() {
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
    }

    @Test
    public void gathersEventsFromSuppressedByKeys() {
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));
        prerequisites.put("B", Collections.singleton("C"));
        prerequisites.put("C", Collections.emptySet());

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("C", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> prerequisites = new HashMap<>();
        Set<String> aDeps = new HashSet<>();
        aDeps.add("B");
        aDeps.add("C");
        prerequisites.put("A", aDeps);

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        Set<String> aSuppressors = new HashSet<>();
        aSuppressors.add("B");
        aSuppressors.add("C");
        suppressedBy.put("A", aSuppressors);

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
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
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));
        prerequisites.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                prerequisites,
                Collections.emptyMap()
        );
        computer.compute(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsCircularDependencyThroughSuppression() {
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));
        suppressedBy.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                Collections.emptyMap(),
                suppressedBy
        );
        computer.compute(); // Should throw
    }

    @Test(expected = IllegalStateException.class)
    public void detectsCircularDependencyThroughMixedRelationships() {
        // A depends on B (prerequisite), B suppressed by A
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("B", Collections.singleton("A"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                prerequisites,
                suppressedBy
        );
        computer.compute(); // Should throw
    }

    @Test
    public void includesEventsOnlyInPrerequisiteValues() {
        // B is only mentioned as a prerequisite value, not as a key
        Map<String, Set<String>> prerequisites = new HashMap<>();
        prerequisites.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                prerequisites,
                Collections.emptyMap()
        );
        List<String> result = computer.compute();

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.indexOf("B") < result.indexOf("A"));
    }

    @Test
    public void includesEventsOnlyInSuppressorValues() {
        // B is only mentioned as a suppressor value, not as a key
        Map<String, Set<String>> suppressedBy = new HashMap<>();
        suppressedBy.put("A", Collections.singleton("B"));

        EvaluationOrderComputer<String> computer = new EvaluationOrderComputer<>(
                Collections.emptyMap(),
                suppressedBy
        );
        List<String> result = computer.compute();

        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.indexOf("B") < result.indexOf("A"));
    }
}
