package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

public interface MySegmentsStorageContainer {

    MySegmentsStorage getStorageForKey(String matchingKey);

    @NonNull
    MySegmentsStorage getLargeSegmentsStorageForKey(String matchingKey);

    /**
     * @return Amount of unique segments present in storage.
     */
    long getUniqueAmount();

    /**
     * @return Amount of unique large segments present in storage.
     */
    long getUniqueLargeSegmentsAmount();
}
