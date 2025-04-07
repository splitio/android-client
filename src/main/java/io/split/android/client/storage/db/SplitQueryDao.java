package io.split.android.client.storage.db;

import java.util.List;
import java.util.Map;

public interface SplitQueryDao {
    public List<SplitEntity> get(long rowIdFrom, int maxRows);
    
    /**
     * Get all splits as a Map with name as key and body as value.
     * This is a more efficient way to get splits compared to Room's automatic entity mapping.
     */
    public Map<String, String> getAllAsMap();
}