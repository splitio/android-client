package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MySegmentDao extends SegmentDao<MySegmentEntity> {

    @Override
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(MySegmentEntity mySegment);

    @Override
    @Query("UPDATE my_segments SET user_key = :userKey, segment_list = :segmentList WHERE user_key = :formerUserKey")
    void update(String formerUserKey, String userKey, String segmentList);

    @Override
    @Query("SELECT user_key, segment_list, updated_at FROM my_segments WHERE user_key = :userKey")
    MySegmentEntity getByUserKey(String userKey);

    @Override
    @Query("SELECT user_key, segment_list, updated_at FROM my_segments")
    List<MySegmentEntity> getAll();
}
