# tracker

Self-contained event-tracking module for the Split Android SDK.

## Purpose

Encapsulates the logic for validating and dispatching track events. It is intentionally decoupled from the SDK's internal networking, storage, and telemetry layers — dependencies are injected via callbacks.

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

## Design notes

- `TrackerEvent` is a plain domain object separate from the networking DTO (`Event` in `main/dtos/`). The caller converts between them in the `onEventPush` callback.
- Validator adapters (`EventValidatorImpl`, `PropertyValidatorImpl`, `ValidationMessageLoggerImpl`) implement both the original `main/` interfaces and the tracker interfaces, preserving existing behaviour.
- No dependency on `SyncManager`, `TelemetryStorageProducer`, or any `main/` internals.
