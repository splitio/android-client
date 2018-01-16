package io.split.android.client.cache;

import io.split.android.client.dtos.SplitChange;

/**
 * Created by guillermo on 11/23/17.
 */

public interface ISplitChangeCache {

    /**
     * Adds a change to the splits cache
     * @param splitChange Change to cache
     * @return if it was successfully saved or not
     */
    boolean addChange(SplitChange splitChange);

    /**
     * Gets changes from the splits cache
     * @param since
     * @return the cached changes since the given timestamp
     */
    SplitChange getChanges(long since);
}
