package io.split.android.client.storage.mysegments;

public interface MySegmentsStorageContainer {

    MySegmentsStorage getStorageForKey(String matchingKey);

    /**
     * @return Amount of unique segments present in storage.
     */
    long getUniqueAmount();
}
