package io.split.android.client.service.mysegments;

import java.util.Set;

public interface MySegmentsTaskFactory {

    MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache, Long targetSegmentsCn, Long targetLargeSegmentsCn);

    LoadMySegmentsTask createLoadMySegmentsTask();

    MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber);

    MySegmentsUpdateTask createMyLargeSegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber);
}
