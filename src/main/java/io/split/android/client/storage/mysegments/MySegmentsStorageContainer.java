package io.split.android.client.storage.mysegments;

import io.split.android.client.storage.RolloutDefinitionsCache;

public interface MySegmentsStorageContainer extends RolloutDefinitionsCache {

    MySegmentsStorage getStorageForKey(String matchingKey);

    /**
     * @return Amount of unique segments present in storage.
     */
    long getUniqueAmount();
}
