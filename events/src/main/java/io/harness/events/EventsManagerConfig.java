package io.harness.events;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains the interdependencies between events and internal events.
 *
 * @param <E> external events type
 * @param <I> internal events type
 */
public final class EventsManagerConfig<E, I> {
    // External events that require ALL listed internals (AND)
    private final Map<E, Set<I>> mRequireAll;
    // External events triggered by ANY of the listed internal groups (OR of ANDs)
    private final Map<E, Set<Set<I>>> mRequireAny;
    // External-event guards: prerequisites that must have fired before External can emit
    private final Map<E, Set<E>> mPrerequisites;
    // External-event guards: if any of these have fired, suppress E
    private final Map<E, Set<E>> mSuppressedBy;
    // Execution policy: max executions per external event (-1 = unlimited)
    private final Map<E, Integer> mExecutionLimits;
    // Topologically sorted evaluation order (prerequisites and suppressors come before dependents)
    private final List<E> mEvaluationOrder;

    /**
     * Creates a new EventsManagerConfig.
     *
     * @param requireAll      External events that require ALL listed internals (AND)
     * @param requireAny      External events triggered by ANY of the listed internal groups (OR of ANDs)
     * @param prerequisites   External-event guards: prerequisites that must have fired before External can emit
     * @param suppressedBy    External-event guards: if any of these have fired, suppress E
     * @param executionLimits Execution policy: max executions per external event (-1 = unlimited)
     */
    private EventsManagerConfig(Map<E, Set<I>> requireAll,
                                Map<E, Set<Set<I>>> requireAny,
                                Map<E, Set<E>> prerequisites,
                                Map<E, Set<E>> suppressedBy,
                                Map<E, Integer> executionLimits) {
        mRequireAll = requireAll == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(requireAll));
        mRequireAny = requireAny == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(requireAny));
        mPrerequisites = prerequisites == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(prerequisites));
        mSuppressedBy = suppressedBy == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(suppressedBy));
        mExecutionLimits = executionLimits == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(executionLimits));
        
        mEvaluationOrder = computeEvaluationOrder();
    }

    public static <I, E> EventsManagerConfig<E, I> empty() {
        return new EventsManagerConfig<>(Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    /**
     * Computes the topological sort of external events based on prerequisites and suppression.
     * 
     * Edge direction: If A depends on B (prerequisite or suppression), then B -> A (B must come before A).
     * 
     * @return topologically sorted list of events
     * @throws IllegalStateException if a circular dependency is detected
     */
    private List<E> computeEvaluationOrder() {
        // Step 1: Gather all events from all sources
        Set<E> allEvents = new HashSet<>();
        allEvents.addAll(mRequireAll.keySet());
        allEvents.addAll(mRequireAny.keySet());
        allEvents.addAll(mPrerequisites.keySet());
        allEvents.addAll(mSuppressedBy.keySet());
        allEvents.addAll(mExecutionLimits.keySet());
        
        // Also include events that appear as values (prerequisites, suppressors)
        for (Set<E> prereqs : mPrerequisites.values()) {
            allEvents.addAll(prereqs);
        }
        for (Set<E> suppressors : mSuppressedBy.values()) {
            allEvents.addAll(suppressors);
        }
        
        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Step 2: Build adjacency list for the dependency graph
        // For each event, track which events must come before it
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
        
        // Step 3: Perform topological sort using DFS with cycle detection
        List<E> result = new ArrayList<>();
        Set<E> visited = new HashSet<>();
        Set<E> visiting = new HashSet<>(); // For cycle detection
        
        for (E event : allEvents) {
            if (!visited.contains(event)) {
                visit(event, dependencies, visited, visiting, result);
            }
        }
        
        return Collections.unmodifiableList(result);
    }
    
    /**
     * DFS visit helper for topological sort with cycle detection.
     * 
     * @param node the current node to visit
     * @param dependencies the dependency graph (node -> set of nodes that must come before it)
     * @param visited set of permanently visited nodes
     * @param visiting set of nodes currently being visited (for cycle detection)
     * @param result the sorted result list
     * @throws IllegalStateException if a cycle is detected
     */
    private void visit(E node, Map<E, Set<E>> dependencies, Set<E> visited, Set<E> visiting, List<E> result) {
        if (visited.contains(node)) {
            return; // Already processed
        }
        
        if (visiting.contains(node)) {
            throw new IllegalStateException("Circular dependency detected involving event: " + node);
        }
        
        visiting.add(node);
        
        // Visit all dependencies first (events that must come before this one)
        Set<E> deps = dependencies.get(node);
        if (deps != null) {
            for (E dep : deps) {
                visit(dep, dependencies, visited, visiting, result);
            }
        }
        
        visiting.remove(node);
        visited.add(node);
        result.add(node);
    }

    @NotNull
    public Map<E, Set<I>> getRequireAll() {
        return mRequireAll;
    }

    @NotNull
    public Map<E, Set<Set<I>>> getRequireAny() {
        return mRequireAny;
    }

    @NotNull
    public Map<E, Set<E>> getPrerequisites() {
        return mPrerequisites;
    }

    @NotNull
    public Map<E, Set<E>> getSuppressedBy() {
        return mSuppressedBy;
    }

    @NotNull
    public Map<E, Integer> getExecutionLimits() {
        return mExecutionLimits;
    }

    @NotNull
    public List<E> getEvaluationOrder() {
        return mEvaluationOrder;
    }

    /**
     * Creates a new Builder for EventsManagerConfig.
     *
     * @param <E> external events type
     * @param <I> internal events type
     * @return a new Builder instance
     */
    public static <E, I> Builder<E, I> builder() {
        return new Builder<>();
    }

    /**
     * Builder for EventsManagerConfig.
     *
     * @param <E> external events type
     * @param <I> internal events type
     */
    public static final class Builder<E, I> {
        private final Map<E, Set<I>> mRequireAll = new HashMap<>();
        private final Map<E, Set<Set<I>>> mRequireAny = new HashMap<>();
        private final Map<E, Set<E>> mPrerequisites = new HashMap<>();
        private final Map<E, Set<E>> mSuppressedBy = new HashMap<>();
        private final Map<E, Integer> mExecutionLimits = new HashMap<>();

        private Builder() {
        }

        /**
         * Adds a requirement that ALL specified internal events must occur for the external event to fire.
         *
         * @param externalEvent  the external event
         * @param internalEvents the internal events that must ALL occur
         * @return this builder
         */
        @SafeVarargs
        public final Builder<E, I> requireAll(E externalEvent, I... internalEvents) {
            mRequireAll.put(externalEvent, new HashSet<>(Arrays.asList(internalEvents)));
            return this;
        }

        /**
         * Adds a requirement that ANY of the specified internal events will trigger the external event.
         * Each internal event is treated as a group of one (singleton).
         *
         * @param externalEvent  the external event
         * @param internalEvents the internal events, any of which will trigger the external event
         * @return this builder
         */
        @SafeVarargs
        public final Builder<E, I> requireAny(E externalEvent, I... internalEvents) {
            // Convert each individual event to a singleton Set (group of one)
            Set<Set<I>> groups = new HashSet<>();
            for (I internalEvent : internalEvents) {
                groups.add(Collections.singleton(internalEvent));
            }
            mRequireAny.put(externalEvent, groups);
            return this;
        }

        /**
         * Adds a requirement that ANY of the specified internal event groups will trigger the external event.
         * Each group is an AND:  all events in the group must occur.
         * The external event fires when ANY group is fully satisfied (OR of ANDs).
         * <p>
         * Example:
         * <pre>
         * .requireAny(DISH_SERVED,
         *     Set.of(BOUGHT_INGREDIENTS, COOKED_MEAL),                    // Fresh cooking path
         *     Set.of(ORDERED_DELIVERY, DELIVERY_ARRIVED))                 // Delivery path
         * // Fires when: (fresh cooking done) OR (delivery arrived)
         * </pre>
         *
         * @param externalEvent       the external event
         * @param internalEventGroups the groups of internal events; all events in a group must occur (AND),
         *                            and any group being satisfied triggers the external event (OR)
         * @return this builder
         */
        @SafeVarargs
        public final Builder<E, I> requireAny(E externalEvent, Set<I>... internalEventGroups) {
            Set<Set<I>> groups = new HashSet<>(Arrays.asList(internalEventGroups));
            mRequireAny.put(externalEvent, groups);
            return this;
        }

        /**
         * Adds a prerequisite: the external event can only fire after the prerequisite event has fired.
         *
         * @param externalEvent     the external event
         * @param prerequisiteEvent the event that must fire first
         * @return this builder
         */
        public Builder<E, I> prerequisite(E externalEvent, E prerequisiteEvent) {
            Set<E> set = mPrerequisites.get(externalEvent);
            if (set == null) {
                set = new HashSet<>();
                mPrerequisites.put(externalEvent, set);
            }
            set.add(prerequisiteEvent);
            return this;
        }

        /**
         * Adds a suppressor: the external event will be suppressed if the suppressor event has already fired.
         *
         * @param externalEvent   the external event
         * @param suppressorEvent the event that suppresses the external event
         * @return this builder
         */
        public Builder<E, I> suppressedBy(E externalEvent, E suppressorEvent) {
            Set<E> set = mSuppressedBy.get(externalEvent);
            if (set == null) {
                set = new HashSet<>();
                mSuppressedBy.put(externalEvent, set);
            }
            set.add(suppressorEvent);
            return this;
        }

        /**
         * Sets the execution limit for an external event.
         *
         * @param externalEvent the external event
         * @param limit         max executions (-1 = unlimited, 1 = once only)
         * @return this builder
         */
        public Builder<E, I> executionLimit(E externalEvent, int limit) {
            mExecutionLimits.put(externalEvent, limit);
            return this;
        }

        /**
         * Builds the EventsManagerConfig.
         *
         * @return the built config
         */
        public EventsManagerConfig<E, I> build() {
            return new EventsManagerConfig<>(
                    mRequireAll.isEmpty() ? null : mRequireAll,
                    mRequireAny.isEmpty() ? null : mRequireAny,
                    mPrerequisites.isEmpty() ? null : mPrerequisites,
                    mSuppressedBy.isEmpty() ? null : mSuppressedBy,
                    mExecutionLimits.isEmpty() ? null : mExecutionLimits
            );
        }
    }
}
