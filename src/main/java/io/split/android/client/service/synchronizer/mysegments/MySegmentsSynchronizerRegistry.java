package io.split.android.client.service.synchronizer.mysegments;

public interface MySegmentsSynchronizerRegistry {

    void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer);

    void registerMyLargeSegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer);

    void unregisterMySegmentsSynchronizer(String userKey);

    interface Tasks {

        enum SegmentType {
            SEGMENT,
            LARGE_SEGMENT
        }

        void loadMySegmentsFromCache(SegmentType segmentType);

        void synchronizeMySegments(SegmentType segmentType);

        void forceMySegmentsSync(SegmentType segmentType, Long syncDelay);

        void destroy();

        void scheduleSegmentsSyncTask(SegmentType segmentType);

        void submitMySegmentsLoadingTask(SegmentType segmentType);

        void stopPeriodicFetching();

        void expireCache();
    }
}
