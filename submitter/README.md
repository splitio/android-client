# submitter

Generic batch recorder task abstraction.

## Purpose

Encapsulates the logic for submitting batched data (such as impressions and events) to the backend. It provides a reusable abstraction for recorder tasks, decoupled from the SDK's internal storage and networking layers. Dependencies are injected via callbacks.

## Design notes

- For now depends on `events-domain` for the executor types.
- Depends on `logger` for logging.
