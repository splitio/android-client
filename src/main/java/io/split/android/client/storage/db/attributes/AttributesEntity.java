package io.split.android.client.storage.db.attributes;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attributes")
public class AttributesEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_key")
    private String userKey;

    @ColumnInfo(name = "attributes")
    private String attributes;

    @NonNull
    public String getUserKey() {
        return userKey;
    }

    @NonNull
    public String getAttributes() {
        return attributes;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }
}
