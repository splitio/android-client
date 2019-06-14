package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.Split;

/**
 * Created by guillermo on 11/23/17.
 */

public interface ISplitCache {
    /**
     *
     * @param split Split
     * @return Whether the Split was added or not
     */
    boolean addSplit(Split split);

    /**
     *
     * @param changeNumber Change number of the last fetch
     * @return Whether the change number was saved or not
     */
    boolean setChangeNumber(long changeNumber);

    /**
     *
     * @return The last fetched change number
     */
    long getChangeNumber();

    /**
     *
     * @param splitName Split Name
     * @return Split JSON representation
     */
    Split getSplit(String splitName);

    /**
     * Get all cached splits names
     * @return List containing all splits names
     */
    List<String> getSplitNames();

    /**
     * Indicates if a traffic type exists for the environment
     * @return true if it exists, false otherwise
     */
    boolean trafficTypeExists(String trafficType);
}
