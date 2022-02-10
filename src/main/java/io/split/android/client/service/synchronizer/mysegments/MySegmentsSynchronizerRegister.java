package io.split.android.client.service.synchronizer.mysegments;

public interface MySegmentsSynchronizerRegister {

    void registerMySegmentsSynchronizer(String userKey, MySegmentsSynchronizer mySegmentsSynchronizer);

    void unregisterMySegmentsSynchronizer(String userKey);
}
