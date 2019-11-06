package io.split.android.client.storage.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "general_info")
public class GeneralInfoEntity {

    @PrimaryKey()
    @NonNull
    private String name;

    private String stringValue;

    private long longValue;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public GeneralInfoEntity() {
    }

    @Ignore
    public GeneralInfoEntity(@NonNull String name, String stringValue) {
        this.name = name;
        this.stringValue = stringValue;
    }

    @Ignore
    public GeneralInfoEntity(@NonNull String name, long longValue) {
        this.name = name;
        this.longValue = longValue;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
