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

### Why It's Necessary

When a single internal event notification could trigger multiple external events, they must be evaluated in the correct order based on their dependencies (`prerequisite` and `suppressedBy` relationships).

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

### Benefits

This ensures correctness and validates there are no cycles as a bonus.

### Implementation

The topological sort treats both `prerequisite` and `suppressedBy` as dependency edges in a graph:
- If A has `prerequisite` B → B must be evaluated before A
- If A is `suppressedBy` B → B must be evaluated before A

This ensures that when checking whether an event can fire, all events it depends on have already been processed.
