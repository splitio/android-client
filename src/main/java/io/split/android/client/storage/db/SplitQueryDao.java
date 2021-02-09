package io.split.android.client.storage.db;

import java.util.List;

public interface SplitQueryDao {
    public List<SplitEntity> get(long rowIdFrom, int maxRows);
}