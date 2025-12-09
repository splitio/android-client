package io.harness.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the evaluation order of events based on their prerequisites and suppression relationships.
 * <p>
 * Prerequisites and suppressions imply a dependency between events, so prerequisites and
 * suppressors need to be evaluated before their dependents.
 *
 * @param <E> event type
 */
final class EvaluationOrderComputer<E> {

    private final Set<E> mAllEvents;
    private final Map<E, Set<E>> mPrerequisites;
    private final Map<E, Set<E>> mSuppressedBy;

    /**
     * Creates a new EvaluationOrderComputer.
     *
     * @param allEvents     all events that need to be included in the evaluation order
     * @param prerequisites map from event to its prerequisites (events that must fire before it)
     * @param suppressedBy  map from event to its suppressors (events that, if fired, suppress it)
     */
    EvaluationOrderComputer(Set<E> allEvents, Map<E, Set<E>> prerequisites, Map<E, Set<E>> suppressedBy) {
        mAllEvents = allEvents != null ? allEvents : Collections.emptySet();
        mPrerequisites = prerequisites != null ? prerequisites : Collections.emptyMap();
        mSuppressedBy = suppressedBy != null ? suppressedBy : Collections.emptyMap();
    }

    /**
     * Computes the topological sort of events based on prerequisites and suppression.
     * <p>
     * Edge direction: If A depends on B (prerequisite or suppression), then B -> A (B must come before A).
     *
     * @return topologically sorted list of events
     * @throws IllegalStateException if a circular dependency is detected
     */
    List<E> compute() {
        Set<E> allEvents = gatherAllEvents();

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

        Map<E, Set<E>> dependencies = buildDependencyGraph(allEvents);

        return new TopologicalSorter<>(allEvents, dependencies).sort();
    }

    /**
     * Gathers all events that need to be in the evaluation order.
     * This includes all configured events plus any events referenced in prerequisites/suppression.
     */
    private Set<E> gatherAllEvents() {
        Set<E> allEvents = new HashSet<>(mAllEvents);

        // Also include events that appear as values in prerequisites/suppression
        // (they might not be configured themselves but need to be evaluated first)
        for (Set<E> prereqs : mPrerequisites.values()) {
            allEvents.addAll(prereqs);
        }
        for (Set<E> suppressors : mSuppressedBy.values()) {
            allEvents.addAll(suppressors);
        }

        return allEvents;
    }

    /**
     * Builds the dependency graph from prerequisites and suppression relationships.
     * <p>
     * For each event, tracks which events must come before it.
     * <p>
     * For example, the following configuration:
     * <pre>
     * A -> B // B is a prerequisite for A
     * B -> C // B is suppressed by C
     * </pre>
     * Will result in the following dependency graph:
     * <pre>
     * {
     *   A: [B], // A depends on B
     *   B: [C], // B depends on C
     *   C: [], // C has no dependencies
     * }
     * </pre>
     */
    private Map<E, Set<E>> buildDependencyGraph(Set<E> allEvents) {
        Map<E, Set<E>> dependencies = new HashMap<>();
        for (E event : allEvents) {
            dependencies.put(event, new HashSet<>());
        }

        // Add edges: if A has prerequisite B, then B -> A (B must come before A)
        for (Map.Entry<E, Set<E>> entry : mPrerequisites.entrySet()) {
            E dependent = entry.getKey();
            for (E prerequisite : entry.getValue()) {
                dependencies.get(dependent).add(prerequisite);
            }
        }

        // Add edges: if A is suppressed by B, then B -> A (B must come before A)
        for (Map.Entry<E, Set<E>> entry : mSuppressedBy.entrySet()) {
            E suppressed = entry.getKey();
            for (E suppressor : entry.getValue()) {
                dependencies.get(suppressed).add(suppressor);
            }
        }

        return dependencies;
    }
}

