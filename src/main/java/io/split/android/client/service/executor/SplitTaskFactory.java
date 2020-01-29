package io.split.android.client.service.executor;

public interface SplitTaskFactory {
    SplitTask createEventsRecorderTask();

    SplitTask createImpressionsRecorderTask();

    SplitTask createSplitsSyncTask();

    SplitTask createMySegmentsSyncTask();
}
