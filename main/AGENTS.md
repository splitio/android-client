# AGENTS.md — main module

## Purpose

Core SDK logic for the Split Android SDK. This is the largest and most complex module, containing:
- `SplitClient` / `SplitFactory` implementation
- Feature flag evaluation engine (matchers, splitter, parser)
- Data synchronization (splits, segments, SSE streaming)
- Impressions and events tracking
- Storage (database via Room/SQLite)
- Telemetry, lifecycle management, localhost mode

## Key Packages

| Package | Description |
|---------|-------------|
| `io.split.android.client` | Top-level: SplitClientImpl, SplitFactoryImpl, SplitClientConfig, EvaluatorImpl |
| `io.split.android.client.api` | Public API implementations |
| `io.split.android.client.service` | Background services: splits sync, segments sync, SSE, impressions, events, telemetry |
| `io.split.android.client.service.executor` | Task executor and SplitTaskFactory |
| `io.split.android.client.service.sseclient` | SSE client and BackoffCounterTimer |
| `io.split.android.client.service.synchronizer` | Sync orchestration |
| `io.split.android.client.impressions` | Impression capture, deduplication, flushing |
| `io.split.android.client.factory` | SplitFactoryImpl and SplitFactoryHelper |
| `io.split.android.client.localhost` | Localhost mode (YAML/JSON feature flag files) |
| `io.split.android.client.shared` | SplitClientContainer, shared state |
| `io.split.android.engine` | Evaluation engine: experiments (ParsedSplit), matchers, splitter |
| `io.split.android.engine.experiments` | SplitParser, ParsedSplit, FetcherPolicy |
| `io.split.android.engine.matchers` | All matcher implementations (string, set, number, semver, date) |

## Testing

- **Run unit tests**: `./gradlew :main:test`
- **Run a single class**: `./gradlew :main:test --tests "io.split.android.client.<TestClass>"`
- **Test sources**: `main/src/test/java/` + `main/src/sharedTest/java/` (shared with instrumented tests)
- **Instrumented tests**: `main/src/androidTest/java/` — requires device/emulator farm (Sauce Labs)
- **Test options**: `unitTests.returnDefaultValues = true` (mocks Android framework returns)

## Dependencies

This module depends on all other modules:
- `:api` — public interfaces
- `:logger` — logging
- `:http`, `:http-api` — networking
- `:fallback` — fallback treatments
- `:events`, `:events-domain` — event processing
- `:backoff` — retry logic
- `:tracker` — impression/event tracking

## Important Patterns

- **Task pattern**: Background work is modeled as `SplitTask` implementations, scheduled via `SplitTaskExecutor`
- **Factory pattern**: `SplitTaskFactoryImpl` wires together all tasks; `SplitClientFactoryImpl` creates clients
- **SSE streaming**: SSE client with `BackoffCounterTimer` (now from `:backoff` module) for reconnect
- **Shared state**: `SplitClientContainer` manages multiple `SplitClient` instances (multi-key support)
- **Localhost mode**: Reads feature flags from YAML/JSON files without connecting to Split servers

## DOs

- Follow the existing `SplitTask` interface when adding new background tasks
- Maintain the `SplitTaskFactory` interface in `:api` when adding new task types
- Add corresponding unit tests in `src/test/` for all new evaluator/matcher logic
- Use `RetryBackoffCounterTimerFactory` for retry-capable timers (delegates to `:backoff` module)

## DON'Ts

- Don't add direct dependencies on Android UI framework (this is a library, not an app)
- Don't put public API interfaces in this module — they belong in `:api`
- Don't add instrumented-test-only dependencies to the main `dependencies` block
