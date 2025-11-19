package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MyLargeSegmentDao extends SegmentDao<MyLargeSegmentEntity> {

    String TABLE_NAME = "my_large_segments";

    @Override
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(MyLargeSegmentEntity mySegment);

    @Override
    @Query("UPDATE " + TABLE_NAME + " SET user_key = :userKey, segment_list = :segmentList WHERE user_key = :formerUserKey")
    void update(String formerUserKey, String userKey, String segmentList);

    @Override
    @Query("SELECT user_key, segment_list, updated_at FROM " + TABLE_NAME + " WHERE user_key = :userKey")
    MyLargeSegmentEntity getByUserKey(String userKey);

    @Override
    @Query("SELECT user_key, segment_list, updated_at FROM " + TABLE_NAME)
    List<MyLargeSegmentEntity> getAll();
}
