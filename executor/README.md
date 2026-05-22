# executor

Generic task scheduling and execution infrastructure for the Split Android SDK.

## Purpose

Provides a pausable task executor with support for scheduled and immediate task execution, parallel task execution with timeout, serial and batch task wrappers, main thread task execution via Android Handler, and pause/resume/stop controls.

## Usage

### Basic Task Execution

```java
SplitTaskExecutor executor = new SplitTaskExecutorImpl();

SplitTask task = () -> {
    // Do work
    return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
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
