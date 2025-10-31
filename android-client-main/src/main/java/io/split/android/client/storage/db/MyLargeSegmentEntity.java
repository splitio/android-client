package io.split.android.client.storage.db;

import androidx.room.Entity;

@Entity(tableName = "my_large_segments")
public class MyLargeSegmentEntity extends SegmentEntity {

    public static final Creator<MyLargeSegmentEntity> CREATOR = new Creator<MyLargeSegmentEntity>() {
        @Override
        public MyLargeSegmentEntity createEntity(String userKey, String segmentList, long updatedAt) {
            MyLargeSegmentEntity entity = new MyLargeSegmentEntity();
            entity.setUserKey(userKey);
            entity.setSegmentList(segmentList);
            entity.setUpdatedAt(updatedAt);

            return entity;
        }
    };

    public static Creator<MyLargeSegmentEntity> creator() {
        return CREATOR;
    }
}
