package io.split.android.client.storage.db;

import androidx.room.Entity;

@Entity(tableName = "my_segments")
public class MySegmentEntity extends SegmentEntity {

    private final static Creator<MySegmentEntity> CREATOR = new Creator<MySegmentEntity>() {
        @Override
        public MySegmentEntity createEntity(String userKey, String segmentList, long updatedAt) {
            MySegmentEntity entity = new MySegmentEntity();
            entity.setUserKey(userKey);
            entity.setSegmentList(segmentList);
            entity.setUpdatedAt(updatedAt);

            return entity;
        }
    };

    public static Creator<MySegmentEntity> creator() {
        return CREATOR;
    }
}
