package io.split.android.client.localhost;

import io.split.android.client.storage.mysegments.EmptyMySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageContainer;

public class LocalhostMySegmentsStorageContainer implements MySegmentsStorageContainer {

    private final MySegmentsStorage mEmptyMySegmentsStorage = new EmptyMySegmentsStorage();

    @Override
    public MySegmentsStorage getStorageForKey(String matchingKey) {
        return mEmptyMySegmentsStorage;
    }

    @Override
    public long getUniqueAmount() {
        return mEmptyMySegmentsStorage.getAll().size();
    }

    @Override
    public void loadLocal() {
        // no-op
    }

    @Override
    public void clear() {
        // No-op
    }
}
