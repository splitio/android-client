package io.split.android.client.cache;

import java.util.List;

/**
 * Created by guillermo on 11/23/17.
 */

public interface ISplitCache {
    /**
     *
     * @param splitName Split Name
     * @param split Split JSON representation
     * @return Whether the Split was added or not
     */
    boolean addSplit(String splitName, String split);

    /**
     *
     * @param splitName Split Name
     * @return Whether the Split was removed or not
     */
    boolean removeSplit(String splitName);

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
    String getSplit(String splitName);

    /**
     * Get all cached splits names
     * @return List containing all splits names
     */
    List<String> getSplitNames();
}
