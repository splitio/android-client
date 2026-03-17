# submitter

Generic batch recorder task abstraction.

## Purpose

Encapsulates the logic for submitting batched data (such as impressions and events) to the backend. It provides a reusable abstraction for recorder tasks, decoupled from the SDK's internal storage and networking layers. Dependencies are injected via callbacks.

## Design notes

- Depends on `events-domain` for shared domain types.
- Depends on `logger` for logging.
- No dependency on `main/` internals or networking implementation details.
