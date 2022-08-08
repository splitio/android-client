package io.split.android.client.service.synchronizer.mysegments;

public interface MySegmentsSynchronizerRegistry {

    void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer);

    void unregisterMySegmentsSynchronizer(String userKey);
}
