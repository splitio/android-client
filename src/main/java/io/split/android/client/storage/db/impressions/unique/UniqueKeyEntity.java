package io.split.android.client.storage.db.impressions.unique;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import io.split.android.client.dtos.Identifiable;

@Entity(tableName = "unique_keys")
public class UniqueKeyEntity implements Identifiable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @NonNull
    private long id;

    @ColumnInfo(name = "user_key")
    @NonNull
    private String userKey;

    @ColumnInfo(name = "feature_list")
    private String featureList;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "status")
    private int status;

    public UniqueKeyEntity() {

    }

    public UniqueKeyEntity(@NonNull String userKey, String featureList, long createdAt, int status) {
        this.userKey = userKey;
        this.featureList = featureList;
        this.createdAt = createdAt;
        this.status = status;
    }

    @NonNull
    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(@NonNull String userKey) {
        this.userKey = userKey;
    }

    public String getFeatureList() {
        return featureList;
    }

    public void setFeatureList(String featureList) {
        this.featureList = featureList;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
