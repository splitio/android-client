package io.split.android.client.storage.db;

import java.util.List;

public interface SegmentDao<T> {

    void update(T mySegment);

    void update(String formerUserKey, String userKey, String segmentList);

    T getByUserKey(String userKey);

    List<T> getAll();
}
