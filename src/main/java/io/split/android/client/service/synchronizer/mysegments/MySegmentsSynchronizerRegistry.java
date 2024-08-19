package io.split.android.client.service.synchronizer.mysegments;

public interface MySegmentsSynchronizerRegistry {

    void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer);

    void unregisterMySegmentsSynchronizer(String userKey);

    interface Tasks {
        enum SegmentType {
            SEGMENT,
            LARGE_SEGMENT
        }

        void loadMySegmentsFromCache();

        void synchronizeMySegments();

        void forceMySegmentsSync(Long syncDelay);

        void destroy();

        void scheduleSegmentsSyncTask();

        void submitMySegmentsLoadingTask();

        void stopPeriodicFetching();
    }
}
