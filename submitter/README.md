# submitter

Generic batch recorder task abstraction for the Split Android SDK.

## Purpose

Encapsulates the logic for submitting batched data (such as impressions and events) to the Split platform. It provides a reusable abstraction for recorder tasks, decoupled from the SDK's internal storage and networking layers — dependencies are injected via callbacks.

## Design notes

- Depends on `events-domain` for shared domain types.
- Depends on `logger` for logging.
- No dependency on `main/` internals or networking implementation details.
