package io.split.android.client.service.executor;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface SplitTaskFactory {
    SplitTask createEventsRecorderTask();

    SplitTask createImpressionsRecorderTask();

    SplitTask createSplitsSyncTask();

    SplitTask createMySegmentsSyncTask();

    SplitTask createLoadMySegmentsTask();

    SplitTask createLoadSplitsTask();

    SplitTask createSseAuthenticationTask();

    SplitTask createSplitKillTask();

    SplitTask createMySegmentsUpdateTask();

    SplitTask createSplitsUpdateTask();

}
