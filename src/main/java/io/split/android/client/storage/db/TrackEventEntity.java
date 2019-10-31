package io.split.android.client.storage.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "track_events")
public class TrackEventEntity {
    public final static int STATUS_ACTIVE = 0;
    public final static int STATUS_DELETED = 1;

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String body;

    @NonNull
    private long timestamp;
    private int status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    public void setBody(@NonNull String body) {
        this.body = body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
