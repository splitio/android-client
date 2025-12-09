package io.harness.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs topological sorting of nodes based on their dependencies.
 *
 * @param <T> the type of nodes to sort
 */
final class TopologicalSorter<T> {

    private final Set<T> mNodes;
    private final Map<T, Set<T>> mDependencies;

    /**
     * Creates a new TopologicalSorter.
     *
     * @param nodes        all nodes to be sorted
     * @param dependencies map from each node to the set of nodes it depends on
     *                     (i.e., nodes that must come before it)
     */
    TopologicalSorter(Set<T> nodes, Map<T, Set<T>> dependencies) {
        mNodes = nodes == null ? Collections.emptySet() : nodes;
        mDependencies = dependencies == null ? Collections.emptyMap() : dependencies;
    }

    /**
     * Computes the topological sort of the nodes.
     * <p>
     * The result is ordered such that for any node A that depends on node B,
     * B will appear before A in the returned list.
     * <p>
     * For example, the following dependency graph:
     * <p>
     * ```
     * A -> B // B is a prerequisite for A
     * B -> C // C is suppressed by B
     * ```
     * <p>
     * Will result in the following sorted list:
     * <p>
     * ```
     * [C, B, A]
     * ```
     *
     * @return topologically sorted list of nodes
     * @throws IllegalStateException if a circular dependency is detected
     */
    List<T> sort() {
        if (mNodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Set<T> visiting = new HashSet<>(); // For cycle detection

        for (T node : mNodes) {
            if (!visited.contains(node)) {
                visit(node, visited, visiting, result);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Visit all dependencies first (nodes that must come before this one),
     * then add the current node to the result list.
     * <p>
     * If a cycle is detected, an exception is thrown.
     *
     * @param node     the current node to visit
     * @param visited  set of permanently visited nodes
     * @param visiting set of nodes currently being visited (for cycle detection)
     * @param result   the sorted result list
     * @throws IllegalStateException if a cycle is detected
     */
    private void visit(T node, Set<T> visited, Set<T> visiting, List<T> result) {
        if (visited.contains(node)) {
            return; // Already processed
        }

        if (visiting.contains(node)) {
            throw new IllegalStateException("Circular dependency detected involving node: " + node);
        }

        visiting.add(node);

        // Visit all dependencies first (nodes that must come before this one)
        Set<T> deps = mDependencies.get(node);
        if (deps != null) {
            for (T dep : deps) {
                visit(dep, visited, visiting, result);
            }
        }

        visiting.remove(node);
        visited.add(node);
        result.add(node);
    }
}
