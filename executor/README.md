# executor

Generic task scheduling and execution infrastructure for the Split Android SDK.

## Purpose

Provides a pausable, lifecycle-aware task executor with support for:
- Scheduled and immediate task execution
- Parallel task execution with timeout
- Serial and batch task wrappers
- Main thread task execution via Android Handler
- Pause/resume support for Android lifecycle management

## Public API

### Core Executor

| Class / Interface | Role |
|---|---|
| `SplitTaskExecutor` | Main interface for task scheduling and execution |
| `SplitTaskExecutorImpl` | Default implementation with configurable thread pool |
| `SplitSingleThreadTaskExecutor` | Single-threaded variant for sequential execution |
| `SplitBaseTaskExecutor` | Abstract base with pause/resume and lifecycle management |

### Task Abstractions

| Class / Interface | Role |
|---|---|
| `SplitTask` | Task interface with single `execute()` method |
| `SplitTaskType` | Enum of 18 task types (SPLITS_SYNC, EVENTS_RECORDER, etc.) |
| `SplitTaskExecutionInfo` | Execution result with status, type, and optional data |
| `SplitTaskExecutionStatus` | SUCCESS or ERROR status enum |
| `SplitTaskExecutionListener` | Callback interface for task completion |

### Parallel Execution

| Class / Interface | Role |
|---|---|
| `SplitParallelTaskExecutor<T>` | Interface for parallel task execution with timeout |
| `SplitParallelTaskExecutorImpl<T>` | Implementation using ExecutorService.invokeAll() |
| `SplitParallelTaskExecutorFactory` | Factory for creating parallel executors |

### Wrappers & Utilities

| Class / Interface | Role |
|---|---|
| `TaskWrapper` | Wraps SplitTask with execution listener callback |
| `SplitTaskSerialWrapper` | Executes multiple tasks serially, stops on first error |
| `SplitTaskBatchWrapper` | Batch execution wrapper for multiple tasks |
| `ThreadFactoryBuilder` | Creates named daemon threads for executor |

### Pausable Schedulers

| Class / Interface | Role |
|---|---|
| `PausableScheduledThreadPoolExecutor` | Interface extending ScheduledExecutorService with pause/resume |
| `PausableScheduledThreadPoolExecutorImpl` | Implementation with lifecycle-aware scheduling |
| `PausableThreadPoolExecutor` | Non-scheduled pausable executor interface |
| `PausableThreadPoolExecutorImpl` | Non-scheduled pausable executor implementation |

## Usage

### Basic Task Execution

```java
SplitTaskExecutor executor = new SplitTaskExecutorImpl();

SplitTask task = () -> {
    // Do work
    return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
};

executor.submit(task, null);
```

### Scheduled Execution

```java
executor.schedule(
    task,
    60,  // delay in seconds
    null  // optional listener
);
```

### Parallel Execution

```java
SplitParallelTaskExecutor<Result> parallelExecutor =
    new SplitParallelTaskExecutorFactoryImpl().build();

List<Callable<Result>> tasks = Arrays.asList(
    () -> fetchData1(),
    () -> fetchData2()
);

List<Result> results = parallelExecutor.executeParallelTasks(tasks, 60);
```

### Lifecycle Management

```java
executor.pause();   // Pause scheduled tasks
executor.resume();  // Resume scheduled tasks
executor.stop();    // Stop and shutdown executor
```

## Dependencies

- **logger**: Logging abstraction
- **Android framework**: Handler/Looper for main thread execution
- **AndroidX annotations**: @NonNull, @Nullable, etc.

## Wiring (in main module)

Created in `SplitFactoryImpl`:

```java
SplitTaskExecutor executor = new SplitTaskExecutorImpl();
SplitTaskExecutor sseExecutor = new SplitSingleThreadTaskExecutor();
```
