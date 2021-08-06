package io.split.android.client.service.executor;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.SaveImpressionsCountTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsRemovalTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;

public interface SplitTaskFactory {
    EventsRecorderTask createEventsRecorderTask();

    ImpressionsRecorderTask createImpressionsRecorderTask();

    SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration);

    MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache);

    LoadMySegmentsTask createLoadMySegmentsTask();

    LoadSplitsTask createLoadSplitsTask();

    SplitKillTask createSplitKillTask(Split split);

    MySegmentsUpdateTask createMySegmentsUpdateTask(List<String> segments);

    MySegmentsRemovalTask createMySegmentsRemovalTask(String segment);

    SplitsUpdateTask createSplitsUpdateTask(long since);

    FilterSplitsInCacheTask createFilterSplitsInCacheTask();

    CleanUpDatabaseTask createCleanUpDatabaseTask(long maxTimestamp);

    SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> count);

    ImpressionsCountRecorderTask createImpressionsCountRecorderTask();

}
