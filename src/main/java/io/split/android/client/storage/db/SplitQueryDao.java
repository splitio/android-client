package io.split.android.client.storage.db;

import java.util.List;
import java.util.Map;

public interface SplitQueryDao {
    List<SplitEntity> get(long rowIdFrom, int maxRows);
    Map<String, String> getAllAsMap();
}