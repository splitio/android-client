package io.split.android.client.service.synchronizer.mysegments;

import io.split.android.client.api.Key;

public interface MySegmentsSynchronizerRegistry {

    void registerMySegmentsSynchronizer(Key key, MySegmentsSynchronizer mySegmentsSynchronizer);

    void unregisterMySegmentsSynchronizer(Key key);
}
