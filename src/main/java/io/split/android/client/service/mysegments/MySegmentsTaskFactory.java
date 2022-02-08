package io.split.android.client.service.mysegments;

import java.util.List;

public interface MySegmentsTaskFactory {

    MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache);

    LoadMySegmentsTask createLoadMySegmentsTask();

    MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments);

    MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, String segmentName);
}
