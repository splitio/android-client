# tracker

Self-contained event-tracking module.

## Purpose

Encapsulates the logic for validating and dispatching track events. Dependencies are injected via callbacks.

## Public API

| Class / Interface | Role |
|---|---|
| `Tracker` | Primary interface. `enableTracking(boolean)` / `track(...)` |
| `DefaultTracker` | Default implementation |
| `TrackerEvent` | Domain object representing a validated event (no serialization concerns) |
| `TrackerEventValidator` | Validates key, traffic type, event type, value |
| `TrackerPropertyValidator` | Validates event properties; returns `TrackerPropertyResult` |
| `TrackerLogger` | Logging abstraction (`log`, `e`, `v`) |
| `TrackerValidationError` | Simple error/warning result (`isError`, `getMessage`) |

## Wiring (in `main/`)

`DefaultTracker` is wired in `SplitFactoryImpl.EventsTrackerProvider`:

```java
new DefaultTracker(
    new EventValidatorImpl(keyValidator, splitsStorage),  // implements TrackerEventValidator
    new ValidationMessageLoggerImpl(),                    // implements TrackerLogger
    new PropertyValidatorImpl(),                          // implements TrackerPropertyValidator
    trackerEvent -> {
        // convert TrackerEvent → Event DTO, then push
        mSyncManager.pushEvent(toEvent(trackerEvent));
    },
    latencyMs -> mTelemetryStorage.recordLatency(Method.TRACK, latencyMs)
);
```

The `onTrackLatency` callback is optional (pass `null` to skip telemetry).
