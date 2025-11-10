package io.split.android.client.storage.db.rbs;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "rule_based_segments")
public class RuleBasedSegmentEntity {

    /** @noinspection NotNullFieldNotInitialized*/
    @PrimaryKey
    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "body")
    private String body;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    /** @noinspection unused*/
    // room constructor
    public RuleBasedSegmentEntity() {

    }

    @Ignore
    public RuleBasedSegmentEntity(String name, String body, long updatedAt) {
        this.name = name;
        this.body = body;
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
