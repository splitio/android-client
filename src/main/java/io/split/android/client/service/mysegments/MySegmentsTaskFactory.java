package io.split.android.client.service.mysegments;

import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;

public interface MySegmentsTaskFactory {

    MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache, Long targetSegmentsCn, Long targetLargeSegmentsCn);

    LoadMySegmentsTask createLoadMySegmentsTask();

    // only used for mySegments v1
    MySegmentsOverwriteTask createMySegmentsOverwriteTask(SegmentsChange segmentsChange);

    MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber);

    MySegmentsUpdateTask createMyLargeSegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber);
}
