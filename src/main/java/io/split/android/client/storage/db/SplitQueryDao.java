package io.split.android.client.storage.db;

import java.util.Map;

public interface SplitQueryDao {
    Map<String, SplitEntity> getAllAsMap();
}