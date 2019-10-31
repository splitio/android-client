package io.split.android.client.storage.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "my_segments")
public class MySegmentEntity {

    @PrimaryKey()
    @NonNull
    @ColumnInfo(name = "user_key")
    private String userKey;

    @NonNull
    @ColumnInfo(name = "segment_list")
    private String segmentList;

    @NonNull
    private long timestamp;

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
