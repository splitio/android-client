package io.split.android.client.service.synchronizer.mysegments;

import io.split.android.client.service.mysegments.MySegmentUpdateParams;

public interface MySegmentsSynchronizer {

    void loadMySegmentsFromCache();

    void synchronizeMySegments();

    void forceMySegmentsSync(MySegmentUpdateParams params);

    void destroy();

    void scheduleSegmentsSyncTask();

    void submitMySegmentsLoadingTask();

    void stopPeriodicFetching();
}
