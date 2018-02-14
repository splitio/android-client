package io.split.android.engine.segments;

/**
 * Created by guillermo on 12/26/17.
 */

public interface MySegments {
    /**
     * This method MUST NOT throw any exceptions.
     *
     * @return true if mySegments contains the segment name. false otherwise.
     */
    boolean contains(String segmentName);

    /**
     * Forces a sync of the mySegments with the remote server, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();
}
