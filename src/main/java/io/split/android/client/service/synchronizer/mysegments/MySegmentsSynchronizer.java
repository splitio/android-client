package io.split.android.client.service.synchronizer.mysegments;

public interface MySegmentsSynchronizer {

    void loadMySegmentsFromCache();

    void synchronizeMySegments();

    void forceMySegmentsSync(Long syncDelay);

    void destroy();

    void scheduleSegmentsSyncTask();

    void submitMySegmentsLoadingTask();

    void stopPeriodicFetching();

    void expireCache();
}
