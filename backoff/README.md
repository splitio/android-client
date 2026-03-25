# Backoff module

This module contains the backoff counter logic for the Split SDK.

It provides the types used to calculate retry delays in HTTP infrastructure components such as `RetryableHttpClient`.

Key types:
- `BackoffCounter` — interface with `getNextRetryTime()` and `resetCounter()`
- `ExponentialBackoffCounter` — exponential backoff implementation (base * 2^attempt, capped at a configurable max)
- `FixedIntervalBackoffCounter` — fixed-interval implementation (no-op reset)

## Usage

**Exponential backoff** (doubles each attempt, capped at 30 minutes by default):

```java
BackoffCounter counter = new ExponentialBackoffCounter(1); // base of 1 second

long delay = counter.getNextRetryTime(); // 1s
delay = counter.getNextRetryTime();      // 2s
delay = counter.getNextRetryTime();      // 4s
delay = counter.getNextRetryTime();      // 8s
// ... capped at 1800s (30 min)

counter.resetCounter(); // start over
delay = counter.getNextRetryTime();      // 1s again
```

A custom cap can be specified via the two-argument constructor:

```java
BackoffCounter counter = new ExponentialBackoffCounter(1, /* maxTimeLimit= */ 60);
```

**Fixed-interval backoff** (always returns the same delay, `resetCounter()` is a no-op):

```java
BackoffCounter counter = new FixedIntervalBackoffCounter(5); // 5 seconds

long delay = counter.getNextRetryTime(); // 5s
delay = counter.getNextRetryTime();      // 5s
```
