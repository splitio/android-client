package io.split.android.client.service.executor;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.attributes.LoadAttributesTask;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.SaveImpressionsCountTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsOverwriteTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.mysegments.MySegmentsUpdateTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;

public interface SplitTaskFactory extends TelemetryTaskFactory, MySegmentsTaskFactory {
    EventsRecorderTask createEventsRecorderTask();

    ImpressionsRecorderTask createImpressionsRecorderTask();

    SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration);

    LoadSplitsTask createLoadSplitsTask();

    SplitKillTask createSplitKillTask(Split split);

    SplitsUpdateTask createSplitsUpdateTask(long since);

    FilterSplitsInCacheTask createFilterSplitsInCacheTask();

    CleanUpDatabaseTask createCleanUpDatabaseTask(long maxTimestamp);

    SaveImpressionsCountTask createSaveImpressionsCountTask(List<ImpressionsCountPerFeature> count);

    ImpressionsCountRecorderTask createImpressionsCountRecorderTask();

    LoadAttributesTask createLoadAttributesTask(boolean persistentAttributesEnabled);

}
