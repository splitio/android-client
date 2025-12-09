# Events module

This module provides a generic events management system.

Allows the definition of internal and external events interdependencies, as well as registration.

## Core Concepts

### Internal vs External Events

- **Internal Events**: Low-level events triggered by the system (e.g., data loaded, sync completed)
- **External Events**: High-level events exposed to consumers (e.g., SDK_READY, SDK_UPDATE)

### Event Configuration

Events are configured using `EventsManagerConfig.Builder`:

- **`requireAll(external, internal...)`**: External event fires when ALL internal events have occurred
- **`requireAny(external, internal...)`**: External event fires when ANY internal event occurs
- **`requireAny(external, Set<internal>...)`**: OR-of-ANDs pattern; fires when any group is fully satisfied
- **`prerequisite(external, prerequisiteExternal)`**: External event can only fire after the prerequisite external event has fired
- **`suppressedBy(external, suppressorExternal)`**: External event is permanently suppressed if the suppressor external event has already fired
- **`executionLimit(external, limit)`**: Max times the event can fire (-1 = unlimited, 1 = once only)

## Topological Sort for Evaluation Order

The events system uses **topological sorting** to determine the order in which external events are evaluated. This is essential for correctness.

### Evaluation Flow

1. **Internal Event Arrives**: A single internal event can potentially satisfy conditions for multiple external events.
2. **Single-Pass Evaluation**: The system iterates through a pre-computed list of external events (`mEvaluationOrder`).
3. **Order Matters**: This list is topologically sorted so that events with dependencies (prerequisites/suppression) come *after* the events they depend on.

### Why It's Necessary

When a single internal event notification could trigger multiple external events, they must be evaluated in the correct order based on their dependencies.

#### Prerequisite Example

```
SDK_READY_FROM_CACHE  ←prerequisite←  SDK_READY
```

If both events' conditions are satisfied by the same internal event:

- **Without sort**: If `SDK_READY` is checked first, `prerequisitesSatisfied()` returns `false` because `SDK_READY_FROM_CACHE` hasn't fired yet. `SDK_READY` misses its chance to fire in this cycle.
- **With sort**: `SDK_READY_FROM_CACHE` is evaluated first, fires, then `SDK_READY` sees its prerequisite satisfied and fires—all in one pass.

#### SuppressedBy Example

```
SDK_READY  ──suppressedBy──►  SDK_READY_TIMED_OUT
```

If both events' conditions are satisfied by the same internal event:

- **Without sort**: If `SDK_READY_TIMED_OUT` is checked first, `isSuppressed()` returns `false` because `SDK_READY` hasn't fired yet. Both events fire incorrectly.
- **With sort**: `SDK_READY` is evaluated first, fires, then `SDK_READY_TIMED_OUT` sees it's suppressed and doesn't fire.

### Implementation Details

The sorting logic is split into:

- **`EventsManagerConfig`**: Holds the raw configuration.
- **`EvaluationOrderComputer`**: Gathers all configured events and builds the dependency graph based on prerequisites and suppressors.
- **`TopologicalSorter`**: A generic utility that performs the DFS-based topological sort with cycle detection.

The topological sort treats both `prerequisite` and `suppressedBy` as dependency edges:
- If A has `prerequisite` B → B must be evaluated before A
- If A is `suppressedBy` B → B must be evaluated before A

**Note:** All configured events are included in the evaluation order, even those without dependencies. Independent events can appear anywhere in the list relative to each other, but always before/after their dependents/dependencies as required.
