package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.MySegment;

/**
 * Created by guillermo on 11/23/17.
 */

public interface IMySegmentsCache {

    /**
     * Saves the list of MySegments in cache
     * @param mySegments List of MySegments to cache
     * @return if it was successfully saved or not
     */
    boolean saveMySegments(List<MySegment> mySegments);

    /**
     * Gets MySegments from the cache
     * @return the cached list of MySegments
     */
    List<MySegment> getMySegments();
}
