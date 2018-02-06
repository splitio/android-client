package io.split.android.engine.experiments;

import java.util.List;

public interface SplitFetcher {
    ParsedSplit fetch(String splitName);

    List<ParsedSplit> fetchAll();

    /**
     * Forces a sync of splits, outside of any scheduled
     * syncs. This method MUST NOT throw any exceptions.
     */
    void forceRefresh();
}
