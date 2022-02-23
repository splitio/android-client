package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MySegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(MySegmentEntity mySegment);

    @Query("SELECT user_key, segment_list, updated_at FROM my_segments WHERE user_key = :userKey")
    MySegmentEntity getByUserKey(String userKey);
}
