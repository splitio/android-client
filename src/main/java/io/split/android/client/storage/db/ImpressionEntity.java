package io.split.android.client.storage.db;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import io.split.android.client.dtos.Identifiable;

@Entity(tableName = "impressions")
public class ImpressionEntity implements Identifiable {


    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "test_name")
    private String testName;

    @NonNull
    private String body;

    @ColumnInfo(name = "created_at")
    private long createdAt;
    private int status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTestName() {
        return testName;
    }

    public void setTestName(@NonNull String testName) {
        this.testName = testName;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    public void setBody(@NonNull String body) {
        this.body = body;
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
}
