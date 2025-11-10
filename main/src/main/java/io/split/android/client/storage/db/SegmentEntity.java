package io.split.android.client.storage.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

/**
 * Base class for segment entities. This entity does not correspond to any actual SQL table
 */
public abstract class SegmentEntity {

    @PrimaryKey()
    @NonNull
    @ColumnInfo(name = "user_key")
    private String userKey;

    @NonNull
    @ColumnInfo(name = "segment_list")
    private String segmentList;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @NonNull
    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    @NonNull
    public String getSegmentList() {
        return segmentList;
    }

    public void setSegmentList(@NonNull String segmentList) {
        this.segmentList = segmentList;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Creator<?> creator() {
        return null;
    }

    public interface Creator<T extends SegmentEntity> {
        T createEntity(String userKey, String segmentList, long updatedAt);
    }
}
