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
| `EventPushListener` | Callback interface invoked with validated events (required) |
| `TrackLatencyListener` | Callback interface for track latency telemetry (optional) |
| `TrackExceptionListener` | Callback interface for tracking exceptions (optional) |

## Wiring (in `main/`)

`DefaultTracker` is wired in `SplitFactoryImpl.EventsTrackerProvider`:

```java
new DefaultTracker(
    new EventValidatorImpl(keyValidator, splitsStorage),  // implements TrackerEventValidator
    new ValidationMessageLoggerImpl(),                    // implements TrackerLogger
    new PropertyValidatorImpl(),                          // implements TrackerPropertyValidator
    trackerEvent -> {                                      // EventPushListener (required)
        // convert TrackerEvent → Event DTO, then push
        mSyncManager.pushEvent(toEvent(trackerEvent));
    },
    latencyMs -> mTelemetryStorage.recordLatency(Method.TRACK, latencyMs),  // TrackLatencyListener (optional, can be null)
    () -> mTelemetryStorage.recordException(Method.TRACK)  // TrackExceptionListener (optional, can be null)
);
```

The `TrackLatencyListener` and `TrackExceptionListener` callbacks are optional (pass `null` to skip telemetry).
