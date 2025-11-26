package io.harness.events;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    // External events triggered by ANY of the listed internals (OR)
    private final Map<E, Set<I>> mRequireAny;
    // External-event guards: prerequisites that must have fired before External can emit
    private final Map<E, Set<E>> mPrerequisites;
    // External-event guards: if any of these have fired, suppress E
    private final Map<E, Set<E>> mSuppressedBy;
    // Execution policy: max executions per external event (-1 = unlimited)
    private final Map<E, Integer> mExecutionLimits;

    /**
     * Creates a new EventsManagerConfig.
     *
     * @param requireAll      External events that require ALL listed internals (AND)
     * @param requireAny      External events triggered by ANY of the listed internals (OR)
     * @param prerequisites   External-event guards: prerequisites that must have fired before External can emit
     * @param suppressedBy    External-event guards: if any of these have fired, suppress E
     * @param executionLimits Execution policy: max executions per external event (-1 = unlimited)
     */
    private EventsManagerConfig(Map<E, Set<I>> requireAll,
                               Map<E, Set<I>> requireAny,
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
    }

    public static <I, E> EventsManagerConfig<E, I> empty() {
        return new EventsManagerConfig<>(Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    @NonNull
    public Map<E, Set<I>> getRequireAll() {
        return mRequireAll;
    }

    @NonNull
    public Map<E, Set<I>> getRequireAny() {
        return mRequireAny;
    }

    @NonNull
    public Map<E, Set<E>> getPrerequisites() {
        return mPrerequisites;
    }

    @NonNull
    public Map<E, Set<E>> getSuppressedBy() {
        return mSuppressedBy;
    }

    @NonNull
    public Map<E, Integer> getExecutionLimits() {
        return mExecutionLimits;
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
        private final Map<E, Set<I>> mRequireAny = new HashMap<>();
        private final Map<E, Set<E>> mPrerequisites = new HashMap<>();
        private final Map<E, Set<E>> mSuppressedBy = new HashMap<>();
        private final Map<E, Integer> mExecutionLimits = new HashMap<>();

        private Builder() {
        }

        /**
         * Adds a requirement that ALL specified internal events must occur for the external event to fire.
         *
         * @param externalEvent    the external event
         * @param internalEvents   the internal events that must ALL occur
         * @return this builder
         */
        @SafeVarargs
        public final Builder<E, I> requireAll(E externalEvent, I... internalEvents) {
            mRequireAll.put(externalEvent, new HashSet<>(Arrays.asList(internalEvents)));
            return this;
        }

        /**
         * Adds a requirement that ANY of the specified internal events will trigger the external event.
         *
         * @param externalEvent    the external event
         * @param internalEvents   the internal events, any of which will trigger the external event
         * @return this builder
         */
        @SafeVarargs
        public final Builder<E, I> requireAny(E externalEvent, I... internalEvents) {
            mRequireAny.put(externalEvent, new HashSet<>(Arrays.asList(internalEvents)));
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
