package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.MySegment;

/**
 * Created by guillermo on 11/23/17.
 */

public interface IMySegmentsCache {

    /**
     * Saves the list of MySegments in cache
     * @param key The key for the list to be saved
     * @param mySegments List of MySegments to cache
     * @return If it was successfully saved or not
     */
    boolean saveMySegments(String key, List<MySegment> mySegments);

    /**
     * Gets MySegments from the cache
     * @param key The key corresponding to the cached segments
     * @return The cached list of MySegments
     */
    List<MySegment> getMySegments(String key);

    /**
     * Deletes the list of MySegments from the cache
     */
    void deleteMySegments();
}
