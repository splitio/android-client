package io.split.android.client.storage.db.attributes;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "attributes")
public class AttributesEntity {

    public AttributesEntity() {

    }

    @Ignore
    public AttributesEntity(String userKey, String attributes, long updatedAt) {
        this.userKey = userKey;
        this.attributes = attributes;
        this.updatedAt = updatedAt;
    }

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_key")
    private String userKey;

    @ColumnInfo(name = "attributes")
    private String attributes;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @NonNull
    public String getUserKey() {
        return userKey;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
