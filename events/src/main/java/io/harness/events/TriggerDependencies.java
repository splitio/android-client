package io.harness.events;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TriggerDependencies<E, I> {
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

    public TriggerDependencies(Map<E, Set<I>> requireAll,
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

    public static <I, E> TriggerDependencies<E, I> empty() {
        return new TriggerDependencies<>(Collections.emptyMap(),
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
}
