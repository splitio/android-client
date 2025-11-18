package io.split.android.client.events;

import static io.split.android.client.events.SplitEvent.SDK_READY;
import static io.split.android.client.events.SplitEvent.SDK_READY_FROM_CACHE;
import static io.split.android.client.events.SplitEvent.SDK_READY_TIMED_OUT;
import static io.split.android.client.events.SplitEvent.SDK_UPDATE;
import static io.split.android.client.events.SplitInternalEvent.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.harness.events.EventsManagerConfig;

public class SplitEventsConfiguration {

    static EventsManagerConfig<SplitEvent, SplitInternalEvent> get() {
        final Map<SplitEvent, Set<SplitInternalEvent>> requireAll = new HashMap<>();

        // SDK_READY: require segments + splits (Updated OR Fetched per source via OR triggers below)
        Set<SplitInternalEvent> readyRequired = new HashSet<>();
        readyRequired.add(MY_SEGMENTS_UPDATED);
        readyRequired.add(SPLITS_UPDATED);
        requireAll.put(SDK_READY, readyRequired);

        // SDK_READY_FROM_CACHE: all storage-load events
        Set<SplitInternalEvent> cacheRequired = new HashSet<>();
        cacheRequired.add(MY_SEGMENTS_LOADED_FROM_STORAGE);
        cacheRequired.add(SPLITS_LOADED_FROM_STORAGE);
        cacheRequired.add(ATTRIBUTES_LOADED_FROM_STORAGE);
        cacheRequired.add(ENCRYPTION_MIGRATION_DONE);
        requireAll.put(SDK_READY_FROM_CACHE, cacheRequired);

        // SDK_UPDATE: OR triggers (any of these after READY)
        Set<SplitInternalEvent> updateTriggers = new HashSet<>();
        updateTriggers.add(MY_SEGMENTS_UPDATED);
        updateTriggers.add(MY_LARGE_SEGMENTS_UPDATED);
        updateTriggers.add(SPLITS_UPDATED);
        updateTriggers.add(RULE_BASED_SEGMENTS_UPDATED);
        updateTriggers.add(SPLIT_KILLED_NOTIFICATION);

        final Map<SplitEvent, Set<SplitInternalEvent>> requireAny = new HashMap<>();
        requireAny.put(SDK_UPDATE, updateTriggers);

        // Guards
        final Map<SplitEvent, Set<SplitEvent>> prerequisites = new HashMap<>();
        Set<SplitEvent> updatePrereqs = new HashSet<>();
        updatePrereqs.add(SDK_READY);  // UPDATE requires READY, not cache-ready
        prerequisites.put(SDK_UPDATE, updatePrereqs);

        final Map<SplitEvent, Set<SplitEvent>> suppressedBy = new HashMap<>();
        Set<SplitEvent> timeoutSuppressors = new HashSet<>();
        timeoutSuppressors.add(SDK_READY);
        suppressedBy.put(SDK_READY_TIMED_OUT, timeoutSuppressors);

        // Execution limits
        final Map<SplitEvent, Integer> executionLimits = new HashMap<>();
        executionLimits.put(SDK_READY, 1);
        executionLimits.put(SDK_READY_FROM_CACHE, 1);
        executionLimits.put(SDK_READY_TIMED_OUT, 1);
        executionLimits.put(SDK_UPDATE, -1);

        return new EventsManagerConfig<>(requireAll,
                requireAny,
                prerequisites,
                suppressedBy,
                executionLimits);
    }
}
