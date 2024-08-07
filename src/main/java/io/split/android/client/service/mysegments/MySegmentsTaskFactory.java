package io.split.android.client.service.mysegments;

import java.util.List;
import java.util.Set;

public interface MySegmentsTaskFactory {

    MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache);

    LoadMySegmentsTask createLoadMySegmentsTask();

    MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments, Long changeNumber);

    MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber);
}
