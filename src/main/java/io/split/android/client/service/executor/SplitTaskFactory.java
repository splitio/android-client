package io.split.android.client.service.executor;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.sseauthentication.SseAuthenticationTask;
import io.split.android.client.service.workmanager.EventsRecorderWorker;

public interface SplitTaskFactory {
    EventsRecorderTask createEventsRecorderTask();

    ImpressionsRecorderTask createImpressionsRecorderTask();

    SplitsSyncTask createSplitsSyncTask();

    MySegmentsSyncTask createMySegmentsSyncTask();

    LoadMySegmentsTask createLoadMySegmentsTask();

    LoadSplitsTask createLoadSplitsTask();

    SseAuthenticationTask createSseAuthenticationTask();

    SplitKillTask createSplitKillTask(Split split);

    MySegmentsUpdateTask createMySegmentsUpdateTask(List<String> segments);

    SplitTask createSplitsUpdateTask();

}
